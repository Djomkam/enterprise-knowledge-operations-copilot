package io.innovation.ekoc.retrieval.service;

import io.innovation.ekoc.retrieval.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorSearchService {

    private final JdbcTemplate jdbcTemplate;

    public List<SearchResult> search(
            float[] queryEmbedding,
            double similarityThreshold,
            int topK,
            List<UUID> documentIds,
            List<UUID> teamIds) {

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

        if (documentIds != null && !documentIds.isEmpty()) {
            sql.append("  AND dc.document_id IN (")
               .append("?,".repeat(documentIds.size() - 1)).append("?)\n");
            documentIds.forEach(id -> params.add(id));
        }

        if (teamIds != null && !teamIds.isEmpty()) {
            sql.append("  AND d.team_id IN (")
               .append("?,".repeat(teamIds.size() - 1)).append("?)\n");
            teamIds.forEach(id -> params.add(id));
        }

        sql.append("ORDER BY dc.embedding <=> CAST(? AS vector) LIMIT ?");
        params.add(vectorLiteral);
        params.add(topK);

        log.debug("Executing vector search: threshold={}, topK={}", similarityThreshold, topK);

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) ->
                SearchResult.builder()
                        .chunkId(UUID.fromString(rs.getString("chunk_id")))
                        .documentId(UUID.fromString(rs.getString("document_id")))
                        .documentTitle(rs.getString("document_title"))
                        .content(rs.getString("content"))
                        .position(rs.getInt("position"))
                        .similarityScore(rs.getDouble("similarity_score"))
                        .metadata(rs.getString("metadata"))
                        .build());
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
