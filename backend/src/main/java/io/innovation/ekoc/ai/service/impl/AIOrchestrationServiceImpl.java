package io.innovation.ekoc.ai.service.impl;

import io.innovation.ekoc.ai.agent.AnswerComposer;
import io.innovation.ekoc.ai.agent.PlannerAgent;
import io.innovation.ekoc.ai.agent.PolicyAgent;
import io.innovation.ekoc.ai.agent.RetrieverAgent;
import io.innovation.ekoc.ai.service.AIOrchestrationService;
import io.innovation.ekoc.chat.dto.ChatRequest;
import io.innovation.ekoc.chat.dto.ChatResponse;
import io.innovation.ekoc.retrieval.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIOrchestrationServiceImpl implements AIOrchestrationService {

    private final PlannerAgent plannerAgent;
    private final RetrieverAgent retrieverAgent;
    private final PolicyAgent policyAgent;
    private final AnswerComposer answerComposer;

    @Override
    public ChatResponse processChat(ChatRequest request, String userId) {
        String query = request.getMessage();
        log.info("Processing chat for user={} query={}", userId, query);

        // Step 1: Plan — determine if retrieval is needed
        PlannerAgent.RetrievalPlan plan = plannerAgent.plan(query, userId);
        log.debug("Planner decision: needsRetrieval={}, reasoning={}", plan.needsRetrieval(), plan.reasoning());

        // Step 2: Retrieve — fetch relevant chunks
        List<SearchResult> context = List.of();
        if (plan.needsRetrieval()) {
            context = retrieverAgent.retrieve(plan.searchRequest(), userId);
        }

        // Step 3: Filter — apply access control
        List<SearchResult> filtered = policyAgent.filter(context, userId);
        log.debug("After policy filter: {}/{} results retained", filtered.size(), context.size());

        // Step 4: Compose — generate grounded answer with citations
        ChatResponse response = answerComposer.compose(
                query,
                filtered,
                request.getConversationId() != null ? buildHistoryHint(request) : null);

        log.info("Chat response composed: {} chars, {} citations",
                response.getContent().length(), response.getCitations() != null ? response.getCitations().size() : 0);

        return response;
    }

    private String buildHistoryHint(ChatRequest request) {
        // Conversation history is prepended by ChatService before calling this method.
        // Returning null here so the composer doesn't double-include it.
        return null;
    }
}
