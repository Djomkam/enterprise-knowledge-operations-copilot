package io.innovation.ekoc.ai.service;

import io.innovation.ekoc.ai.dto.EmbeddingRequest;
import io.innovation.ekoc.ai.dto.EmbeddingResponse;

import java.util.List;

/**
 * Abstraction for embedding generation.
 * Implementation will delegate to Spring AI for embedding model clients.
 *
 * TODO: Implement with Spring AI EmbeddingClient
 * - OpenAI: spring-ai-openai
 * - Ollama: spring-ai-ollama
 */
public interface EmbeddingService {

    /**
     * Generate embedding for a single text input.
     */
    float[] embed(String text);

    /**
     * Generate embeddings for multiple text inputs (batched).
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * Get the dimensionality of the embedding model.
     */
    int getDimension();
}
