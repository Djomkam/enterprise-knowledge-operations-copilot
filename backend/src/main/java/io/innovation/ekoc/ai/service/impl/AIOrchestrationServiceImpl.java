package io.innovation.ekoc.ai.service.impl;

import io.innovation.ekoc.ai.agent.AnswerComposer;
import io.innovation.ekoc.ai.agent.PlannerAgent;
import io.innovation.ekoc.ai.agent.RetrieverAgent;
import io.innovation.ekoc.ai.service.AIOrchestrationService;
import io.innovation.ekoc.analytics.dto.QueryAnalyticsEvent;
import io.innovation.ekoc.chat.dto.ChatRequest;
import io.innovation.ekoc.chat.dto.ChatResponse;
import io.innovation.ekoc.config.RabbitMQConfig;
import io.innovation.ekoc.memory.service.MemoryService;
import io.innovation.ekoc.prompttemplate.service.PromptTemplateService;
import io.innovation.ekoc.retrieval.dto.SearchResult;
import io.innovation.ekoc.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIOrchestrationServiceImpl implements AIOrchestrationService {

    private final PlannerAgent plannerAgent;
    private final RetrieverAgent retrieverAgent;
    private final AnswerComposer answerComposer;
    private final MemoryService memoryService;
    private final PromptTemplateService promptTemplateService;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public ChatResponse processChat(ChatRequest request, String username) {
        long startMs = System.currentTimeMillis();
        String query = request.getMessage();
        log.info("Processing chat for user={} query={}", username, query);

        // Step 1: Plan
        PlannerAgent.RetrievalPlan plan = plannerAgent.plan(query, username);

        // Step 2: Retrieve (with HyDE + re-ranking inside RetrieverAgent)
        List<SearchResult> context = List.of();
        if (plan.needsRetrieval()) {
            context = retrieverAgent.retrieve(plan.searchRequest(), username);
        }
        log.debug("Retriever returned {} results", context.size());

        // Step 3: Memory context
        String memoryContext = null;
        try {
            memoryContext = memoryService.buildContextString(username, query);
        } catch (Exception e) {
            log.warn("Failed to load memory context for user={}: {}", username, e.getMessage());
        }

        // Step 4: Resolve prompt template for this user's team/role
        String systemPrompt = resolveSystemPrompt(username);

        // Step 5: Compose answer
        ChatResponse response = answerComposer.compose(query, context, memoryContext, systemPrompt);

        long latencyMs = System.currentTimeMillis() - startMs;
        log.info("Chat response composed: {} chars, {} citations, {}ms",
                response.getContent().length(),
                response.getCitations() != null ? response.getCitations().size() : 0,
                latencyMs);

        publishAnalytics(username, request, response, context.size(), latencyMs, true, null);
        return response;
    }

    @Override
    public Flux<String> streamChat(ChatRequest request, String username) {
        String query = request.getMessage();
        log.info("Streaming chat for user={} query={}", username, query);

        PlannerAgent.RetrievalPlan plan = plannerAgent.plan(query, username);

        List<SearchResult> context = List.of();
        if (plan.needsRetrieval()) {
            context = retrieverAgent.retrieve(plan.searchRequest(), username);
        }

        String memoryContext = null;
        try {
            memoryContext = memoryService.buildContextString(username, query);
        } catch (Exception e) {
            log.warn("Failed to load memory context for streaming user={}: {}", username, e.getMessage());
        }

        String systemPrompt = resolveSystemPrompt(username);
        return answerComposer.streamCompose(query, context, memoryContext, systemPrompt);
    }

    // -------------------------------------------------------------------------

    private String resolveSystemPrompt(String username) {
        try {
            var user = userRepository.findByUsername(username).orElse(null);
            if (user == null) return null;
            UUID teamId = null; // could be resolved from team membership if needed
            String roleType = user.getRoles().stream().findFirst()
                    .map(r -> r.getName().name()).orElse(null);
            return promptTemplateService.resolveSystemPrompt(teamId, roleType);
        } catch (Exception e) {
            log.warn("Failed to resolve prompt template: {}", e.getMessage());
            return null;
        }
    }

    private void publishAnalytics(String username, ChatRequest request, ChatResponse response,
                                  int retrievalHits, long latencyMs, boolean success, String error) {
        try {
            var user = userRepository.findByUsername(username).orElse(null);
            var event = QueryAnalyticsEvent.builder()
                    .userId(user != null ? user.getId() : null)
                    .conversationId(request.getConversationId())
                    .query(request.getMessage())
                    .responseLength(response != null ? response.getContent().length() : 0)
                    .retrievalHits(retrievalHits)
                    .latencyMs(latencyMs)
                    .tokensUsed(response != null ? response.getTokensUsed() : null)
                    .success(success)
                    .errorMessage(error)
                    .build();
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ANALYTICS_EXCHANGE,
                    RabbitMQConfig.ANALYTICS_ROUTING_KEY,
                    event);
        } catch (Exception e) {
            log.warn("Failed to publish analytics event: {}", e.getMessage());
        }
    }
}
