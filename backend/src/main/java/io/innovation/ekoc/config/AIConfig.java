package io.innovation.ekoc.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AIConfig {

    private String provider;
    private OpenAIConfig openai;
    private OllamaConfig ollama;
    private EmbeddingsConfig embeddings;
    private RetrievalConfig retrieval;

    @Getter
    @Setter
    public static class OpenAIConfig {
        private String apiKey;
        private ModelConfig model;
    }

    @Getter
    @Setter
    public static class OllamaConfig {
        private String baseUrl;
        private ModelConfig model;
    }

    @Getter
    @Setter
    public static class ModelConfig {
        private String chat;
        private String embedding;
    }

    @Getter
    @Setter
    public static class EmbeddingsConfig {
        private Integer dimension;
        private Integer batchSize;
    }

    @Getter
    @Setter
    public static class RetrievalConfig {
        private Integer topK;
        private Double similarityThreshold;
    }
}
