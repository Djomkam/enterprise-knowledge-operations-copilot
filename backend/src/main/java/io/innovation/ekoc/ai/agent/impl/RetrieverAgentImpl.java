package io.innovation.ekoc.ai.agent.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.innovation.ekoc.ai.agent.RetrieverAgent;
import io.innovation.ekoc.ai.dto.ChatCompletionRequest;
import io.innovation.ekoc.ai.service.ChatModelClient;
import io.innovation.ekoc.retrieval.dto.SearchRequest;
import io.innovation.ekoc.retrieval.dto.SearchResult;
import io.innovation.ekoc.retrieval.service.RetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetrieverAgentImpl implements RetrieverAgent {

    private static final int RERANK_MIN_RESULTS = 3;

    private final RetrievalService retrievalService;
    private final JdbcTemplate jdbcTemplate;
    private final ChatModelClient chatModelClient;
    private final ObjectMapper objectMapper;

    @Override
    public List<SearchResult> retrieve(SearchRequest request, String username) {
        log.debug("RetrieverAgent: resolving identity and team membership for user {}", username);

        List<Object[]> rows = jdbcTemplate.query(
                """
                SELECT u.id AS user_id, tm.team_id
                FROM users u
                LEFT JOIN team_members tm ON tm.user_id = u.id AND tm.active = true
                WHERE u.username = ?
                """,
                (rs, rowNum) -> new Object[]{
                        UUID.fromString(rs.getString("user_id")),
                        rs.getString("team_id") != null ? UUID.fromString(rs.getString("team_id")) : null
                },
                username);

        if (rows.isEmpty()) {
            log.warn("RetrieverAgent: no user found for username={}", username);
            return List.of();
        }

        UUID userUuid = (UUID) rows.get(0)[0];
        List<UUID> teamIds = rows.stream()
                .map(r -> (UUID) r[1])
                .filter(id -> id != null)
                .distinct()
                .toList();

        // P2-3: HyDE — generate a hypothetical answer and use it as the retrieval query.
        // This shifts the embedding from a terse question to a passage-length answer,
        // dramatically improving dense retrieval recall for factual queries.
        String retrievalQuery = generateHydeQuery(request.getQuery());

        SearchRequest enriched = SearchRequest.builder()
                .query(retrievalQuery)
                .topK(request.getTopK())
                .similarityThreshold(request.getSimilarityThreshold())
                .documentIds(request.getDocumentIds())
                .teamIds(teamIds.isEmpty() ? null : teamIds)
                .userId(userUuid)
                .build();

        List<SearchResult> results = retrievalService.retrieve(enriched);
        log.debug("RetrieverAgent: retrieved {} results for user {} (HyDE query)", results.size(), username);

        // P2-1: Re-rank results with an LLM cross-encoder when there are enough candidates.
        if (results.size() >= RERANK_MIN_RESULTS) {
            results = rerank(request.getQuery(), results);
            log.debug("RetrieverAgent: re-ranked {} results", results.size());
        }

        return results;
    }

    // -------------------------------------------------------------------------
    // HyDE: Hypothetical Document Embeddings
    // -------------------------------------------------------------------------

    private String generateHydeQuery(String originalQuery) {
        try {
            var request = ChatCompletionRequest.builder()
                    .messages(List.of(
                            ChatCompletionRequest.ChatMessage.builder()
                                    .role("system")
                                    .content("Write a concise hypothetical document passage (2-4 sentences) " +
                                             "that would directly answer the user's question. " +
                                             "Write as if it were extracted from an internal knowledge base. " +
                                             "Do NOT say you don't know — always write a plausible answer.")
                                    .build(),
                            ChatCompletionRequest.ChatMessage.builder()
                                    .role("user")
                                    .content(originalQuery)
                                    .build()))
                    .build();
            String hydeText = chatModelClient.complete(request).getContent();
            log.debug("HyDE generated ({} chars) for query: {}", hydeText.length(), originalQuery);
            return hydeText;
        } catch (Exception e) {
            log.warn("HyDE generation failed, falling back to original query: {}", e.getMessage());
            return originalQuery;
        }
    }

    // -------------------------------------------------------------------------
    // Re-ranking: single LLM call scores all candidates at once
    // -------------------------------------------------------------------------

    private List<SearchResult> rerank(String originalQuery, List<SearchResult> candidates) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("You are a relevance judge. For each passage below, score its relevance ")
                  .append("to the QUERY on a scale of 0–10 (10 = perfectly answers the query).\n\n")
                  .append("QUERY: ").append(originalQuery).append("\n\nPASSAGES:\n");

            for (int i = 0; i < candidates.size(); i++) {
                prompt.append("[").append(i).append("] ")
                      .append(candidates.get(i).getContent(), 0,
                              Math.min(candidates.get(i).getContent().length(), 300))
                      .append("\n");
            }

            prompt.append("\nRespond ONLY with a JSON array of scores in index order, e.g.: [8,3,7,...]");

            var request = ChatCompletionRequest.builder()
                    .messages(List.of(
                            ChatCompletionRequest.ChatMessage.builder()
                                    .role("user").content(prompt.toString()).build()))
                    .build();

            String raw = chatModelClient.complete(request).getContent();
            String json = extractJson(raw);
            JsonNode scores = objectMapper.readTree(json);

            if (!scores.isArray() || scores.size() != candidates.size()) {
                log.warn("Re-ranker score count mismatch (got {}, expected {}), skipping", scores.size(), candidates.size());
                return candidates;
            }

            List<double[]> indexed = IntStream.range(0, candidates.size())
                    .mapToObj(i -> new double[]{i, scores.get(i).asDouble()})
                    .sorted(Comparator.comparingDouble(a -> -a[1]))
                    .toList();

            return indexed.stream()
                    .map(pair -> {
                        SearchResult r = candidates.get((int) pair[0]);
                        return SearchResult.builder()
                                .chunkId(r.getChunkId())
                                .documentId(r.getDocumentId())
                                .documentTitle(r.getDocumentTitle())
                                .content(r.getContent())
                                .position(r.getPosition())
                                .similarityScore(pair[1] / 10.0) // normalise to [0,1]
                                .metadata(r.getMetadata())
                                .build();
                    })
                    .toList();
        } catch (Exception e) {
            log.warn("Re-ranking failed, returning original order: {}", e.getMessage());
            return candidates;
        }
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start >= 0 && end > start) return raw.substring(start, end + 1);
        return raw;
    }
}
