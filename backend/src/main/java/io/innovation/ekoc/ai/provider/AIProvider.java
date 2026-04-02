package io.innovation.ekoc.ai.provider;

/**
 * Marker interface for AI provider implementations.
 * Each provider (OpenAI, Ollama) will implement EmbeddingService and ChatModelClient.
 */
public interface AIProvider {
    String getProviderName();
}
