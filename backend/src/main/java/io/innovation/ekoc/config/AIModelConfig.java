package io.innovation.ekoc.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for AI model beans based on the active provider.
 * Creates ChatModel and EmbeddingModel beans conditionally.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AIModelConfig {

    private final AIConfig aiConfig;

    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
    public OpenAiApi openAiApi() {
        String apiKey = aiConfig.getOpenai().getApiKey();
        log.info("Initializing OpenAI API");
        return new OpenAiApi(apiKey);
    }

    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
    public ChatModel openAiChatModel(OpenAiApi openAiApi) {
        String chatModel = aiConfig.getOpenai().getModel().getChat();
        log.info("Creating OpenAI ChatModel with model: {}", chatModel);
        return new OpenAiChatModel(openAiApi);
    }

    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
    public EmbeddingModel openAiEmbeddingModel(OpenAiApi openAiApi) {
        String embeddingModel = aiConfig.getOpenai().getModel().getEmbedding();
        log.info("Creating OpenAI EmbeddingModel with model: {}", embeddingModel);
        return new OpenAiEmbeddingModel(openAiApi);
    }

    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "ollama")
    public OllamaApi ollamaApi() {
        log.info("Initializing Ollama API with base URL: {}", aiConfig.getOllama().getBaseUrl());
        return new OllamaApi(aiConfig.getOllama().getBaseUrl());
    }

    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "ollama")
    public ChatModel ollamaChatModel(OllamaApi ollamaApi) {
        String chatModel = aiConfig.getOllama().getModel().getChat();
        log.info("Creating Ollama ChatModel with model: {}", chatModel);

        OllamaOptions options = OllamaOptions.create()
                .withModel(chatModel);

        return OllamaChatModel.builder()
                .withOllamaApi(ollamaApi)
                .withDefaultOptions(options)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "ollama")
    public EmbeddingModel ollamaEmbeddingModel(OllamaApi ollamaApi) {
        String embeddingModel = aiConfig.getOllama().getModel().getEmbedding();
        log.info("Creating Ollama EmbeddingModel with model: {}", embeddingModel);

        OllamaOptions options = OllamaOptions.create()
                .withModel(embeddingModel);

        return OllamaEmbeddingModel.builder()
                .withOllamaApi(ollamaApi)
                .withDefaultOptions(options)
                .build();
    }
}
