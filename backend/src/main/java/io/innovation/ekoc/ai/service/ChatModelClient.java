package io.innovation.ekoc.ai.service;

import io.innovation.ekoc.ai.dto.ChatCompletionRequest;
import io.innovation.ekoc.ai.dto.ChatCompletionResponse;

/**
 * Abstraction for chat model interactions.
 * Implementation will delegate to Spring AI ChatClient.
 *
 * TODO: Implement with Spring AI ChatClient
 * - OpenAI: OpenAiChatClient
 * - Ollama: OllamaChatClient
 *
 * Spring AI provides a unified interface for different LLM providers.
 */
public interface ChatModelClient {

    /**
     * Generate a chat completion for the given request.
     */
    ChatCompletionResponse complete(ChatCompletionRequest request);

    /**
     * Stream a chat completion (for future implementation).
     */
    // Flux<ChatCompletionResponse> streamComplete(ChatCompletionRequest request);
}
