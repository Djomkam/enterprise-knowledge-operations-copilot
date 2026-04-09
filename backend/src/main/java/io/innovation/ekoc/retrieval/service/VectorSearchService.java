package io.innovation.ekoc.retrieval.service;

import io.innovation.ekoc.retrieval.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSearchService {

    private final JdbcTemplate jdbcTemplate;

    // RRF constant — standard value from the literature (k=60 balances precision/recall)
    private static final int RRF_K = 60;

    // -------------------------------------------------------------------------
    // Pure vector search (unchanged — used as fallback or when hybrid is disabled)
    // -------------------------------------------------------------------------

    public List<SearchResult> search(
            float[] queryEmbedding,
            double similarityThreshold,
            int topK,
            List<UUID> documentIds,
            List<UUID> teamIds,
            UUID userId) {

        String vectorLiteral = toVectorLiteral(queryEmbedding);

        StringBuilder sql = new StringBuilder("""
                SELECT
                    dc.id              AS chunk_id,
                    dc.document_id,
                    d.title            AS document_title,
                    dc.content,
                    dc.position,
                    dc.metadata,
                    1 - (dc.embedding <=> CAST(? AS vector)) AS similarity_score
                FROM document_chunks dc
                JOIN documents d ON dc.document_id = d.id
                WHERE dc.embedding IS NOT NULL
                  AND 1 - (dc.embedding <=> CAST(? AS vector)) >= ?
                """);

        List<Object> params = new ArrayList<>(List.of(vectorLiteral, vectorLiteral, similarityThreshold));

        appendDocumentFilter(sql, params, documentIds);
        appendAclFilter(sql, params, teamIds, userId);

        sql.append("ORDER BY dc.embedding <=> CAST(? AS vector) LIMIT ?");
        params.add(vectorLiteral);
        params.add(topK);

        log.debug("Executing vector search: threshold={}, topK={}", similarityThreshold, topK);

        return jdbcTemplate.query(sql.toString(), params.toArray(), this::mapRow);
    }

    // -------------------------------------------------------------------------
    // Hybrid search: BM25 (trigram) + vector, fused via Reciprocal Rank Fusion
    // -------------------------------------------------------------------------

    /**
     * Runs a dense (vector) retrieval and a sparse (BM25-style trigram) retrieval in parallel,
     * then fuses the two ranked lists using Reciprocal Rank Fusion (RRF).
     *
     * <p>RRF score = Σ 1/(k + rank_i) where k=60 is the RRF constant.
     * Chunks that appear in both lists score higher than those in only one list.
     */
    public List<SearchResult> hybridSearch(
            float[] queryEmbedding,
            String queryText,
            double similarityThreshold,
            int topK,
            List<UUID> documentIds,
            List<UUID> teamIds,
            UUID userId) {

        // Retrieve more candidates per source so RRF has enough material
        int candidateK = topK * 3;

        List<SearchResult> vectorResults = search(
                queryEmbedding, similarityThreshold, candidateK, documentIds, teamIds, userId);

        List<SearchResult> keywordResults = keywordSearch(
                queryText, candidateK, documentIds, teamIds, userId);

        List<SearchResult> fused = reciprocalRankFusion(vectorResults, keywordResults, topK);

        log.debug("Hybrid search: vector={}, keyword={}, fused={}",
                vectorResults.size(), keywordResults.size(), fused.size());
        return fused;
    }

    // -------------------------------------------------------------------------
    // Keyword (trigram / BM25-approximation) search
    // -------------------------------------------------------------------------

    private List<SearchResult> keywordSearch(
            String queryText,
            int topK,
            List<UUID> documentIds,
            List<UUID> teamIds,
            UUID userId) {

        StringBuilder sql = new StringBuilder("""
                SELECT
                    dc.id              AS chunk_id,
                    dc.document_id,
                    d.title            AS document_title,
                    dc.content,
                    dc.position,
                    dc.metadata,
                    similarity(dc.content, ?) AS similarity_score
                FROM document_chunks dc
                JOIN documents d ON dc.document_id = d.id
                WHERE similarity(dc.content, ?) > 0.05
                """);

        List<Object> params = new ArrayList<>(List.of(queryText, queryText));

        appendDocumentFilter(sql, params, documentIds);
        appendAclFilter(sql, params, teamIds, userId);

        sql.append("ORDER BY similarity_score DESC LIMIT ?");
        params.add(topK);

        log.debug("Executing keyword search: topK={}", topK);

        return jdbcTemplate.query(sql.toString(), params.toArray(), this::mapRow);
    }

    // -------------------------------------------------------------------------
    // Reciprocal Rank Fusion
    // -------------------------------------------------------------------------

    private List<SearchResult> reciprocalRankFusion(
            List<SearchResult> vectorList,
            List<SearchResult> keywordList,
            int topK) {

        Map<UUID, Double> rrfScores = new LinkedHashMap<>();
        Map<UUID, SearchResult> resultMap = new LinkedHashMap<>();

        addRrfScores(rrfScores, resultMap, vectorList);
        addRrfScores(rrfScores, resultMap, keywordList);

        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(topK)
                .map(e -> {
                    SearchResult original = resultMap.get(e.getKey());
                    return SearchResult.builder()
                            .chunkId(original.getChunkId())
                            .documentId(original.getDocumentId())
                            .documentTitle(original.getDocumentTitle())
                            .content(original.getContent())
                            .position(original.getPosition())
                            .similarityScore(e.getValue()) // RRF score
                            .metadata(original.getMetadata())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private void addRrfScores(
            Map<UUID, Double> rrfScores,
            Map<UUID, SearchResult> resultMap,
            List<SearchResult> ranked) {

        for (int i = 0; i < ranked.size(); i++) {
            SearchResult r = ranked.get(i);
            double contribution = 1.0 / (RRF_K + i + 1);
            rrfScores.merge(r.getChunkId(), contribution, Double::sum);
            resultMap.putIfAbsent(r.getChunkId(), r);
        }
    }

    // -------------------------------------------------------------------------
    // Shared SQL helpers
    // -------------------------------------------------------------------------

    private void appendDocumentFilter(StringBuilder sql, List<Object> params, List<UUID> documentIds) {
        if (documentIds != null && !documentIds.isEmpty()) {
            sql.append("  AND dc.document_id IN (")
               .append("?,".repeat(documentIds.size() - 1)).append("?)\n");
            documentIds.forEach(params::add);
        }
    }

    private void appendAclFilter(StringBuilder sql, List<Object> params, List<UUID> teamIds, UUID userId) {
        if (teamIds != null && !teamIds.isEmpty()) {
            sql.append("  AND (d.team_id IN (")
               .append("?,".repeat(teamIds.size() - 1)).append("?)")
               .append(" OR d.owner_id = CAST(? AS uuid))\n");
            teamIds.forEach(params::add);
            params.add(userId.toString());
        } else {
            sql.append("  AND d.owner_id = CAST(? AS uuid)\n");
            params.add(userId.toString());
        }
    }

    private SearchResult mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return SearchResult.builder()
                .chunkId(UUID.fromString(rs.getString("chunk_id")))
                .documentId(UUID.fromString(rs.getString("document_id")))
                .documentTitle(rs.getString("document_title"))
                .content(rs.getString("content"))
                .position(rs.getInt("position"))
                .similarityScore(rs.getDouble("similarity_score"))
                .metadata(rs.getString("metadata"))
                .build();
    }

    private String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) sb.append(',');
        }
        sb.append(']');
        return sb.toString();
    }
}
