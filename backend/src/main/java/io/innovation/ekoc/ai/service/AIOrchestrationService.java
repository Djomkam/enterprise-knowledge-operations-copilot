package io.innovation.ekoc.ai.service;

import io.innovation.ekoc.chat.dto.ChatRequest;
import io.innovation.ekoc.chat.dto.ChatResponse;
import reactor.core.publisher.Flux;

public interface AIOrchestrationService {

    /**
     * Process a chat request through the multi-agent pipeline (blocking).
     * Pipeline: PlannerAgent → RetrieverAgent (ACL pre-filtered) → AnswerComposer
     */
    ChatResponse processChat(ChatRequest request, String username);

    /**
     * Stream chat tokens through the same pipeline.
     * Retrieval and planning are blocking; only the LLM answer generation is streamed.
     * Callers are responsible for accumulating tokens and persisting the full response.
     */
    Flux<String> streamChat(ChatRequest request, String username);
}
