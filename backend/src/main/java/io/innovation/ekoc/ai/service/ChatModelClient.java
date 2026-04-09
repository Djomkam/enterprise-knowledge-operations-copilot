package io.innovation.ekoc.ai.service;

import io.innovation.ekoc.ai.dto.ChatCompletionRequest;
import io.innovation.ekoc.ai.dto.ChatCompletionResponse;
import reactor.core.publisher.Flux;

public interface ChatModelClient {

    /**
     * Generate a blocking chat completion.
     */
    ChatCompletionResponse complete(ChatCompletionRequest request);

    /**
     * Stream chat completion tokens for SSE delivery.
     * Each emitted String is a partial content token from the model.
     */
    Flux<String> stream(ChatCompletionRequest request);
}
