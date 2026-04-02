package io.innovation.ekoc.ai.service;

import io.innovation.ekoc.chat.dto.ChatRequest;
import io.innovation.ekoc.chat.dto.ChatResponse;

/**
 * High-level orchestration service that coordinates multi-agent workflow.
 *
 * This service uses LangChain4j for agent orchestration and planning,
 * while delegating to Spring AI for actual LLM/embedding calls.
 *
 * Responsibility separation:
 * - LangChain4j: Agent orchestration, planning, tool use, reasoning
 * - Spring AI: LLM API calls, embeddings, vector store integration
 *
 * TODO: Implement multi-agent orchestration with LangChain4j
 * - Use LangChain4j AiServices for agent abstraction
 * - Use LangChain4j Tools for function calling
 * - Delegate actual LLM calls to Spring AI ChatClient
 */
public interface AIOrchestrationService {

    /**
     * Process a chat request through the multi-agent pipeline:
     * 1. PlannerAgent: Determine retrieval strategy
     * 2. RetrieverAgent: Fetch relevant document chunks
     * 3. PolicyAgent: Apply access control and filters
     * 4. AnswerComposer: Generate grounded response with citations
     */
    ChatResponse processChat(ChatRequest request, String userId);
}
