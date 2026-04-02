package io.innovation.ekoc.ai.agent;

import io.innovation.ekoc.retrieval.dto.SearchRequest;

/**
 * PlannerAgent determines the retrieval strategy for a user query.
 *
 * Responsibilities:
 * - Analyze user intent
 * - Determine if retrieval is needed
 * - Generate search keywords or semantic query
 * - Decide on retrieval parameters (top-k, filters)
 *
 * TODO: Implement with LangChain4j AiServices
 * - Define as an @AiService interface
 * - Use @SystemMessage for agent behavior
 * - Use @UserMessage with placeholders for query analysis
 * - Return structured plan object
 *
 * Example LangChain4j usage:
 * ```
 * @AiService
 * interface PlannerAgent {
 *     @SystemMessage("You are a query planner...")
 *     @UserMessage("Analyze this query: {{query}}")
 *     RetrievalPlan plan(String query);
 * }
 * ```
 */
public interface PlannerAgent {

    /**
     * Analyze query and produce a retrieval plan.
     */
    RetrievalPlan plan(String userQuery, String userId);

    record RetrievalPlan(
            boolean needsRetrieval,
            SearchRequest searchRequest,
            String reasoning
    ) {}
}
