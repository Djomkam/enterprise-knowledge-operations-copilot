package io.innovation.ekoc.ai.service.impl;

import io.innovation.ekoc.ai.service.EmbeddingService;
import io.innovation.ekoc.config.AIConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingServiceImpl(
            AIConfig aiConfig,
            @Autowired(required = false) @Qualifier("openAiEmbeddingModel") EmbeddingModel openAiEmbeddingModel,
            @Autowired(required = false) @Qualifier("ollamaEmbeddingModel") EmbeddingModel ollamaEmbeddingModel) {

        String provider = aiConfig.getProvider();
        this.embeddingModel = switch (provider != null ? provider.toLowerCase() : "") {
            case "openai" -> {
                if (openAiEmbeddingModel == null) throw new IllegalStateException(
                        "ai.provider=openai but no OpenAI EmbeddingModel bean found; check ai.openai.api-key");
                log.info("EmbeddingService using OpenAI");
                yield openAiEmbeddingModel;
            }
            case "ollama" -> {
                if (ollamaEmbeddingModel == null) throw new IllegalStateException(
                        "ai.provider=ollama but no Ollama EmbeddingModel bean found; check ai.ollama.base-url");
                log.info("EmbeddingService using Ollama");
                yield ollamaEmbeddingModel;
            }
            default -> {
                log.warn("ai.provider='{}' — EmbeddingService is non-functional (test/none mode)", provider);
                yield null;
            }
        };
    }

    @Override
    public float[] embed(String text) {
        if (embeddingModel == null) {
            throw new IllegalStateException("No embedding model is configured; set ai.provider to 'openai' or 'ollama'");
        }
        log.debug("Generating embedding for text of length {}", text.length());
        return embeddingModel.embed(text);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (embeddingModel == null) {
            throw new IllegalStateException("No embedding model is configured; set ai.provider to 'openai' or 'ollama'");
        }
        log.debug("Generating embeddings for {} texts", texts.size());
        EmbeddingRequest request = new EmbeddingRequest(texts, null);
        EmbeddingResponse response = embeddingModel.call(request);
        List<float[]> results = new ArrayList<>();
        response.getResults().forEach(embedding -> results.add(embedding.getOutput()));
        return results;
    }

    @Override
    public int getDimension() {
        if (embeddingModel == null) {
            throw new IllegalStateException("No embedding model is configured; set ai.provider to 'openai' or 'ollama'");
        }
        return embeddingModel.dimensions();
    }
}
