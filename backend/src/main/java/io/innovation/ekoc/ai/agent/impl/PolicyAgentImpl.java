package io.innovation.ekoc.ai.agent.impl;

import io.innovation.ekoc.ai.agent.PolicyAgent;
import io.innovation.ekoc.retrieval.dto.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ACL enforcement is now applied pre-query in {@link RetrieverAgentImpl} via the
 * {@code owner_id / team_id} SQL clause in {@link io.innovation.ekoc.retrieval.service.VectorSearchService}.
 * This agent is retained as an audit/logging hook and for future attribute-based policy extensions.
 */
@Slf4j
@Component
public class PolicyAgentImpl implements PolicyAgent {

    @Override
    public List<SearchResult> filter(List<SearchResult> results, String username) {
        log.debug("PolicyAgent: {} results passed pre-query ACL for user={}", results.size(), username);
        return results;
    }

    @Override
    public boolean canAccess(String documentId, String username) {
        // Replaced by pre-query SQL filtering; kept for ad-hoc point checks if needed.
        throw new UnsupportedOperationException(
                "Use VectorSearchService pre-query ACL filtering instead of point canAccess checks");
    }
}
