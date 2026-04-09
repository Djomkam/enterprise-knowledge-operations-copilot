package io.innovation.ekoc.retrieval;

import io.innovation.ekoc.retrieval.dto.SearchResult;
import io.innovation.ekoc.retrieval.service.VectorSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Verifies that Reciprocal Rank Fusion (RRF) correctly boosts chunks that
 * appear in both the vector and keyword result lists.
 */
@ExtendWith(MockitoExtension.class)
class HybridSearchRrfTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private VectorSearchService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new VectorSearchService(jdbcTemplate);
    }

    @Test
    void rrfBoosting_chunkInBothLists_ranksHigherThanChunkInSingleList() {
        UUID shared = UUID.randomUUID();    // appears in both vector and keyword results
        UUID vectorOnly = UUID.randomUUID(); // only in vector results

        SearchResult sharedResult  = result(shared,     "doc1", 0.90);
        SearchResult vectorOnlyResult = result(vectorOnly, "doc2", 0.95);

        // Vector list: vectorOnly first, shared second
        // Keyword list: only shared
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                .thenReturn(List.of(vectorOnlyResult, sharedResult))  // vector search
                .thenReturn(List.of(sharedResult));                    // keyword search

        List<SearchResult> fused = service.hybridSearch(
                new float[]{0.1f}, "query term", 0.5, 5,
                null, List.of(teamId), userId);

        assertThat(fused).hasSizeGreaterThanOrEqualTo(2);

        // shared chunk should rank first because it appears in both lists
        assertThat(fused.get(0).getChunkId()).isEqualTo(shared);
    }

    @Test
    void hybridSearch_topKLimitsOutput() {
        UUID c1 = UUID.randomUUID(), c2 = UUID.randomUUID(), c3 = UUID.randomUUID();

        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                .thenReturn(List.of(result(c1, "d1", 0.9), result(c2, "d2", 0.8), result(c3, "d3", 0.7)))
                .thenReturn(List.of(result(c1, "d1", 0.85), result(c3, "d3", 0.65)));

        List<SearchResult> fused = service.hybridSearch(
                new float[]{0.1f}, "q", 0.5, 2, // topK=2
                null, List.of(teamId), userId);

        assertThat(fused).hasSize(2);
    }

    @Test
    void hybridSearch_emptyKeywordResults_fallsBackToVectorOnly() {
        UUID chunkId = UUID.randomUUID();
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                .thenReturn(List.of(result(chunkId, "doc", 0.9)))
                .thenReturn(List.of()); // keyword returns nothing

        List<SearchResult> fused = service.hybridSearch(
                new float[]{0.1f}, "obscure", 0.5, 5,
                null, List.of(teamId), userId);

        assertThat(fused).hasSize(1);
        assertThat(fused.get(0).getChunkId()).isEqualTo(chunkId);
    }

    // -------------------------------------------------------------------------

    private SearchResult result(UUID chunkId, String docTitle, double score) {
        return SearchResult.builder()
                .chunkId(chunkId)
                .documentId(UUID.randomUUID())
                .documentTitle(docTitle)
                .content("content")
                .position(0)
                .similarityScore(score)
                .metadata("{}")
                .build();
    }
}
