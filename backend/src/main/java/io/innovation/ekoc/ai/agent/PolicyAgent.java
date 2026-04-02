package io.innovation.ekoc.ai.agent;

import io.innovation.ekoc.retrieval.dto.SearchResult;

import java.util.List;

/**
 * PolicyAgent enforces access control and content policies.
 *
 * Responsibilities:
 * - Filter results based on team membership
 * - Enforce document-level permissions
 * - Apply content redaction if needed
 * - Validate that user can access retrieved chunks
 *
 * TODO: Implement with domain service logic
 * - Query user's team memberships
 * - Filter documents by team_id
 * - Apply RBAC rules
 * - Log access attempts for audit
 */
public interface PolicyAgent {

    /**
     * Filter search results based on user permissions.
     */
    List<SearchResult> filter(List<SearchResult> results, String userId);

    /**
     * Check if user can access a specific document.
     */
    boolean canAccess(String documentId, String userId);
}
