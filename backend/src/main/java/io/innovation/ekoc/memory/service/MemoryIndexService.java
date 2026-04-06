package io.innovation.ekoc.memory.service;

import io.innovation.ekoc.ai.service.EmbeddingService;
import io.innovation.ekoc.memory.domain.MemoryEntry;
import io.innovation.ekoc.memory.domain.MemoryType;
import io.innovation.ekoc.memory.dto.MemoryEntryDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryIndexService {

    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbcTemplate;

    public float[] indexMemory(MemoryEntry entry) {
        float[] embedding = embeddingService.embed(entry.getContent());
        log.debug("Indexed memory {} ({} dims)", entry.getId(), embedding.length);
        return embedding;
    }

    public List<MemoryEntryDTO> searchSimilar(String query, UUID userId, MemoryType type, int topK) {
        log.debug("Searching memories for user {} type={} topK={}", userId, type, topK);
        float[] queryEmbedding = embeddingService.embed(query);
        String vectorLiteral = toVectorLiteral(queryEmbedding);

        StringBuilder sql = new StringBuilder("""
                SELECT
                    m.id,
                    m.memory_type,
                    m.content,
                    m.metadata,
                    m.active,
                    m.created_at,
                    1 - (m.embedding <=> CAST(? AS vector)) AS relevance_score
                FROM memory_entries m
                WHERE m.user_id = ?
                  AND m.active = true
                  AND m.embedding IS NOT NULL
                """);

        List<Object> params = new ArrayList<>(List.of(vectorLiteral, userId));

        if (type != null) {
            sql.append("  AND m.memory_type = ?\n");
            params.add(type.name());
        }

        sql.append("ORDER BY m.embedding <=> CAST(? AS vector) LIMIT ?");
        params.add(vectorLiteral);
        params.add(topK);

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) ->
                MemoryEntryDTO.builder()
                        .id(UUID.fromString(rs.getString("id")))
                        .type(MemoryType.valueOf(rs.getString("memory_type")))
                        .content(rs.getString("content"))
                        .metadata(rs.getString("metadata"))
                        .relevanceScore(rs.getDouble("relevance_score"))
                        .active(rs.getBoolean("active"))
                        .createdAt(rs.getTimestamp("created_at").toInstant())
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
