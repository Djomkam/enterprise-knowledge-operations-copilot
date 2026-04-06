package io.innovation.ekoc.ai.service.impl;

import io.innovation.ekoc.ai.agent.AnswerComposer;
import io.innovation.ekoc.ai.agent.PlannerAgent;
import io.innovation.ekoc.ai.agent.PolicyAgent;
import io.innovation.ekoc.ai.agent.RetrieverAgent;
import io.innovation.ekoc.ai.service.AIOrchestrationService;
import io.innovation.ekoc.chat.dto.ChatRequest;
import io.innovation.ekoc.chat.dto.ChatResponse;
import io.innovation.ekoc.memory.service.MemoryService;
import io.innovation.ekoc.retrieval.dto.SearchResult;
import io.innovation.ekoc.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIOrchestrationServiceImpl implements AIOrchestrationService {

    private final PlannerAgent plannerAgent;
    private final RetrieverAgent retrieverAgent;
    private final PolicyAgent policyAgent;
    private final AnswerComposer answerComposer;
    private final MemoryService memoryService;
    private final UserRepository userRepository;

    @Override
    public ChatResponse processChat(ChatRequest request, String userId) {
        String query = request.getMessage();
        log.info("Processing chat for user={} query={}", userId, query);

        // Resolve username once for memory lookup
        String username = userRepository.findById(UUID.fromString(userId))
                .map(u -> u.getUsername())
                .orElse(null);

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

        // Step 4: Build memory context — inject relevant long-term memories as conversation history
        String memoryContext = null;
        if (username != null) {
            try {
                memoryContext = memoryService.buildContextString(username, query);
                if (memoryContext != null) {
                    log.debug("Memory context injected ({} chars) for user={}", memoryContext.length(), username);
                }
            } catch (Exception e) {
                log.warn("Failed to load memory context for user={}: {}", username, e.getMessage());
            }
        }

        // Step 5: Compose — generate grounded answer with citations, enriched by memory context
        ChatResponse response = answerComposer.compose(query, filtered, memoryContext);

        log.info("Chat response composed: {} chars, {} citations",
                response.getContent().length(), response.getCitations() != null ? response.getCitations().size() : 0);

        return response;
    }
}
