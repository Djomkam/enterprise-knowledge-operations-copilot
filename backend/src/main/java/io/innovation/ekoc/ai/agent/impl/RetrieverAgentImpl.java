package io.innovation.ekoc.ai.agent.impl;

import io.innovation.ekoc.ai.agent.RetrieverAgent;
import io.innovation.ekoc.retrieval.dto.SearchRequest;
import io.innovation.ekoc.retrieval.dto.SearchResult;
import io.innovation.ekoc.retrieval.service.RetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetrieverAgentImpl implements RetrieverAgent {

    private final RetrievalService retrievalService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<SearchResult> retrieve(SearchRequest request, String userId) {
        log.debug("RetrieverAgent: fetching team IDs for user {}", userId);

        List<UUID> teamIds = jdbcTemplate.query(
                """
                SELECT tm.team_id FROM team_members tm
                JOIN users u ON tm.user_id = u.id
                WHERE u.username = ? AND tm.active = true
                """,
                (rs, rowNum) -> UUID.fromString(rs.getString("team_id")),
                userId);

        SearchRequest enriched = SearchRequest.builder()
                .query(request.getQuery())
                .topK(request.getTopK())
                .similarityThreshold(request.getSimilarityThreshold())
                .documentIds(request.getDocumentIds())
                .teamIds(teamIds.isEmpty() ? null : teamIds)
                .build();

        List<SearchResult> results = retrievalService.retrieve(enriched);
        log.debug("RetrieverAgent: found {} results for user {}", results.size(), userId);
        return results;
    }
}
