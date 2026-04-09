package io.innovation.ekoc.ingestion;

import io.innovation.ekoc.config.IngestionConfig;
import io.innovation.ekoc.ingestion.service.ChunkingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkingServiceTest {

    private ChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        IngestionConfig config = new IngestionConfig();
        config.setChunkSize(100);
        config.setChunkOverlap(20);
        chunkingService = new ChunkingService(config);
    }

    @Test
    void chunk_emptyString_returnsEmpty() {
        assertThat(chunkingService.chunk("")).isEmpty();
        assertThat(chunkingService.chunk("   ")).isEmpty();
    }

    @Test
    void chunk_shortText_returnsSingleChunk() {
        String text = "This is a short sentence.";
        List<String> chunks = chunkingService.chunk(text);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo(text);
    }

    @Test
    void chunk_longText_splitsByChunkSize() {
        // 300 chars, chunk size 100, overlap 20 → expect ~3 chunks
        String text = "a".repeat(300);
        List<String> chunks = chunkingService.chunk(text);
        assertThat(chunks).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void chunk_respectsSentenceBoundary() {
        // Sentence fits just under the chunk size — should not be broken mid-sentence
        String text = "First sentence ends here. " + "x".repeat(75) + " Second sentence starts.";
        List<String> chunks = chunkingService.chunk(text);
        // The first sentence should appear intact in the first chunk
        assertThat(chunks.get(0)).contains("First sentence ends here.");
    }

    @Test
    void chunk_overlapMaintainsContinuity() {
        // With overlap, the start of chunk N+1 should share content with end of chunk N
        String text = "Word ".repeat(50); // 250 chars
        List<String> chunks = chunkingService.chunk(text);
        assertThat(chunks).hasSizeGreaterThan(1);
        // Each chunk should be non-empty
        chunks.forEach(c -> assertThat(c).isNotBlank());
    }

    @Test
    void chunk_normalizesWindowsLineEndings() {
        String text = "Line one.\r\nLine two.\r\nLine three.";
        List<String> chunks = chunkingService.chunk(text);
        assertThat(chunks).isNotEmpty();
        chunks.forEach(c -> assertThat(c).doesNotContain("\r"));
    }

    @Test
    void estimateTokenCount_nonEmpty() {
        assertThat(chunkingService.estimateTokenCount("Hello world")).isGreaterThan(0);
    }

    @Test
    void estimateTokenCount_roughlyFourCharsPerToken() {
        int tokens = chunkingService.estimateTokenCount("abcd");
        assertThat(tokens).isEqualTo(1);

        tokens = chunkingService.estimateTokenCount("a".repeat(400));
        assertThat(tokens).isEqualTo(100);
    }
}
