package io.innovation.ekoc.retrieval.service;

import io.innovation.ekoc.ai.service.EmbeddingService;
import io.innovation.ekoc.config.AIConfig;
import io.innovation.ekoc.retrieval.dto.SearchRequest;
import io.innovation.ekoc.retrieval.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalService {

    private final EmbeddingService embeddingService;
    private final VectorSearchService vectorSearchService;
    private final AIConfig aiConfig;

    /**
     * Retrieves relevant document chunks for a query using hybrid search
     * (dense vector + sparse BM25-trigram, fused via Reciprocal Rank Fusion).
     */
    public List<SearchResult> retrieve(SearchRequest request) {
        log.debug("Retrieving context for query: {}", request.getQuery());

        float[] queryEmbedding = embeddingService.embed(request.getQuery());

        int topK = request.getTopK() != null
                ? request.getTopK()
                : aiConfig.getRetrieval().getTopK();

        double threshold = request.getSimilarityThreshold() != null
                ? request.getSimilarityThreshold()
                : aiConfig.getRetrieval().getSimilarityThreshold();

        List<SearchResult> results = vectorSearchService.hybridSearch(
                queryEmbedding,
                request.getQuery(),
                threshold,
                topK,
                request.getDocumentIds(),
                request.getTeamIds(),
                request.getUserId());

        log.debug("Retrieved {} results for query (hybrid search)", results.size());
        return results;
    }
}
