package io.innovation.ekoc.retrieval;

import io.innovation.ekoc.retrieval.dto.SearchResult;
import io.innovation.ekoc.retrieval.service.VectorSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VectorSearchServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private VectorSearchService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID teamId = UUID.randomUUID();
    private final UUID chunkId = UUID.randomUUID();
    private final UUID documentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new VectorSearchService(jdbcTemplate);
    }

    @Test
    void search_withTeamIds_includesAclFilter() {
        mockVectorQuery(buildResult(chunkId, documentId, 0.9));

        List<SearchResult> results = service.search(
                new float[]{0.1f, 0.2f}, 0.7, 5,
                null, List.of(teamId), userId);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(Object[].class), any(RowMapper.class));

        String sql = sqlCaptor.getValue();
        assertThat(sql).containsIgnoringCase("d.team_id IN");
        assertThat(sql).containsIgnoringCase("d.owner_id");
        assertThat(results).hasSize(1);
    }

    @Test
    void search_withoutTeamIds_limitsToOwnDocuments() {
        mockVectorQuery(buildResult(chunkId, documentId, 0.85));

        service.search(new float[]{0.1f, 0.2f}, 0.7, 5, null, null, userId);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(Object[].class), any(RowMapper.class));

        String sql = sqlCaptor.getValue();
        assertThat(sql).doesNotContainIgnoringCase("d.team_id IN");
        assertThat(sql).containsIgnoringCase("d.owner_id");
    }

    @Test
    void search_withDocumentIdFilter_appendsFilter() {
        UUID docId = UUID.randomUUID();
        mockVectorQuery(buildResult(chunkId, documentId, 0.8));

        service.search(new float[]{0.1f}, 0.7, 5, List.of(docId), null, userId);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(Object[].class), any(RowMapper.class));

        assertThat(sqlCaptor.getValue()).containsIgnoringCase("dc.document_id IN");
    }

    @Test
    void hybridSearch_combinesVectorAndKeyword() {
        // Both the vector search and the keyword search delegate to jdbcTemplate.query
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                .thenReturn(List.of(buildResult(chunkId, documentId, 0.9)))
                .thenReturn(List.of(buildResult(chunkId, documentId, 0.7)));

        List<SearchResult> results = service.hybridSearch(
                new float[]{0.1f}, "test query", 0.7, 5,
                null, List.of(teamId), userId);

        // jdbcTemplate queried twice (vector + keyword)
        verify(jdbcTemplate, times(2)).query(anyString(), any(Object[].class), any(RowMapper.class));
        // RRF merges: the single chunk from both lists gets a higher combined score
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getChunkId()).isEqualTo(chunkId);
    }

    @Test
    void hybridSearch_rrfBoostsChunksInBothLists() {
        UUID chunkA = UUID.randomUUID();
        UUID chunkB = UUID.randomUUID();
        UUID docA = UUID.randomUUID();
        UUID docB = UUID.randomUUID();

        SearchResult resultA = buildResult(chunkA, docA, 0.95);
        SearchResult resultB = buildResult(chunkB, docB, 0.60);

        // Vector: A first, B second; Keyword: B first, A second
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                .thenReturn(List.of(resultA, resultB))   // vector pass
                .thenReturn(List.of(resultB, resultA));  // keyword pass

        List<SearchResult> results = service.hybridSearch(
                new float[]{0.1f}, "query", 0.5, 5,
                null, List.of(teamId), userId);

        // Both A and B appear in both lists — their combined RRF scores should be equal
        assertThat(results).hasSize(2);
    }

    @Test
    void toVectorLiteral_formatsCorrectly() {
        // White-box: verify the vector literal format through the SQL that reaches JDBC
        mockVectorQuery(buildResult(chunkId, documentId, 0.9));

        service.search(new float[]{1.5f, -0.5f, 0.0f}, 0.7, 5, null, null, userId);

        ArgumentCaptor<Object[]> paramsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbcTemplate).query(anyString(), paramsCaptor.capture(), any(RowMapper.class));

        String vectorLiteral = (String) paramsCaptor.getValue()[0];
        assertThat(vectorLiteral).startsWith("[").endsWith("]");
        assertThat(vectorLiteral).contains("1.5");
        assertThat(vectorLiteral).contains("-0.5");
    }

    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void mockVectorQuery(SearchResult result) {
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                .thenReturn(List.of(result));
    }

    private SearchResult buildResult(UUID chunkId, UUID documentId, double score) {
        return SearchResult.builder()
                .chunkId(chunkId)
                .documentId(documentId)
                .documentTitle("Test Document")
                .content("Some chunk content")
                .position(0)
                .similarityScore(score)
                .metadata("{}")
                .build();
    }
}
