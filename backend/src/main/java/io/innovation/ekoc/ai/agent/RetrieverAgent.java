package io.innovation.ekoc.ai.agent;

import io.innovation.ekoc.retrieval.dto.SearchRequest;
import io.innovation.ekoc.retrieval.dto.SearchResult;

import java.util.List;

/**
 * RetrieverAgent executes semantic retrieval against the vector store.
 *
 * Responsibilities:
 * - Execute vector similarity search
 * - Combine results from multiple sources if needed
 * - Re-rank results based on relevance
 *
 * TODO: Implement using Spring AI PgVectorStore
 * - Inject Spring AI VectorStore
 * - Convert search request to vector query
 * - Use EmbeddingService for query embedding
 * - Apply filters based on user permissions
 *
 * Spring AI provides org.springframework.ai.vectorstore.VectorStore
 * with methods like similaritySearch(SearchRequest)
 */
public interface RetrieverAgent {

    /**
     * Execute retrieval for the given search request.
     */
    List<SearchResult> retrieve(SearchRequest request, String userId);
}
