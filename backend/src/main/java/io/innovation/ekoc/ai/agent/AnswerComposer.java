package io.innovation.ekoc.ai.agent;

import io.innovation.ekoc.chat.dto.ChatResponse;
import io.innovation.ekoc.retrieval.dto.SearchResult;

import java.util.List;

/**
 * AnswerComposer generates grounded responses with citations.
 *
 * Responsibilities:
 * - Construct RAG prompt with context
 * - Call chat model via Spring AI
 * - Extract citations from response
 * - Format final answer with source attribution
 *
 * TODO: Implement with LangChain4j Prompt Templates and Spring AI ChatClient
 * - Use LangChain4j PromptTemplate for RAG prompt construction
 * - Inject Spring AI ChatClient for completion
 * - Parse response and extract citations
 * - Build structured ChatResponse
 *
 * Example flow:
 * 1. Build context from SearchResult list
 * 2. Create prompt: "Based on: {{context}} Answer: {{query}}"
 * 3. Call chatModelClient.complete()
 * 4. Parse and format response with citations
 */
public interface AnswerComposer {

    /**
     * Compose a grounded answer from context and query.
     */
    ChatResponse compose(
            String query,
            List<SearchResult> context,
            String conversationHistory
    );
}
