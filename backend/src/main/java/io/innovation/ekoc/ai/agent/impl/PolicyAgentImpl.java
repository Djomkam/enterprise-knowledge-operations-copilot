package io.innovation.ekoc.ai.agent.impl;

import io.innovation.ekoc.ai.agent.PolicyAgent;
import io.innovation.ekoc.retrieval.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyAgentImpl implements PolicyAgent {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<SearchResult> filter(List<SearchResult> results, String userId) {
        if (results.isEmpty()) {
            return results;
        }

        Set<UUID> accessibleDocumentIds = getAccessibleDocumentIds(userId, results);

        List<SearchResult> filtered = results.stream()
                .filter(r -> accessibleDocumentIds.contains(r.getDocumentId()))
                .collect(Collectors.toList());

        int removed = results.size() - filtered.size();
        if (removed > 0) {
            log.info("PolicyAgent: filtered out {} results for user {} due to access control", removed, userId);
        }

        return filtered;
    }

    @Override
    public boolean canAccess(String documentId, String userId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM documents d
                WHERE d.id = CAST(? AS uuid)
                  AND (
                    d.team_id IS NULL
                    OR EXISTS (
                        SELECT 1 FROM team_members tm
                        JOIN users u ON tm.user_id = u.id
                        WHERE tm.team_id = d.team_id
                          AND u.username = ?
                          AND tm.active = true
                    )
                    OR EXISTS (
                        SELECT 1 FROM users u WHERE u.username = ? AND u.id = d.owner_id
                    )
                  )
                """,
                Integer.class, documentId, userId, userId);

        return count != null && count > 0;
    }

    private Set<UUID> getAccessibleDocumentIds(String userId, List<SearchResult> results) {
        List<UUID> documentIds = results.stream()
                .map(SearchResult::getDocumentId)
                .distinct()
                .collect(Collectors.toList());

        if (documentIds.isEmpty()) {
            return Set.of();
        }

        String placeholders = documentIds.stream()
                .map(id -> "CAST(? AS uuid)")
                .collect(Collectors.joining(","));

        String sql = """
                SELECT d.id FROM documents d
                WHERE d.id IN (%s)
                  AND (
                    d.team_id IS NULL
                    OR EXISTS (
                        SELECT 1 FROM team_members tm
                        JOIN users u ON tm.user_id = u.id
                        WHERE tm.team_id = d.team_id
                          AND u.username = ?
                          AND tm.active = true
                    )
                    OR EXISTS (
                        SELECT 1 FROM users u WHERE u.username = ? AND u.id = d.owner_id
                    )
                  )
                """.formatted(placeholders);

        Object[] params = new Object[documentIds.size() + 2];
        for (int i = 0; i < documentIds.size(); i++) {
            params[i] = documentIds.get(i).toString();
        }
        params[documentIds.size()] = userId;
        params[documentIds.size() + 1] = userId;

        return jdbcTemplate.query(sql, params, (rs, rowNum) ->
                UUID.fromString(rs.getString("id"))
        ).stream().collect(Collectors.toSet());
    }
}
