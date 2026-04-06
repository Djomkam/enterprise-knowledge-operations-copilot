package io.innovation.ekoc.ingestion.service;

import io.innovation.ekoc.config.IngestionConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkingService {

    private final IngestionConfig ingestionConfig;

    public List<String> chunk(String text) {
        int chunkSize = ingestionConfig.getChunkSize();
        int overlap = ingestionConfig.getChunkOverlap();

        String normalized = text.replaceAll("\\r\\n|\\r", "\n").strip();
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        int length = normalized.length();

        while (start < length) {
            int end = Math.min(start + chunkSize, length);

            // Try to break at a paragraph or sentence boundary to avoid mid-word splits
            if (end < length) {
                int paragraphBreak = normalized.lastIndexOf("\n\n", end);
                int sentenceBreak = lastSentenceBreak(normalized, start, end);

                if (paragraphBreak > start + overlap) {
                    end = paragraphBreak;
                } else if (sentenceBreak > start + overlap) {
                    end = sentenceBreak;
                } else {
                    // Fall back to word boundary
                    int wordBreak = normalized.lastIndexOf(' ', end);
                    if (wordBreak > start + overlap) {
                        end = wordBreak;
                    }
                }
            }

            String chunk = normalized.substring(start, end).strip();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            start = end - overlap;
            if (start <= 0 || start >= length) {
                break;
            }
        }

        log.debug("Split text ({} chars) into {} chunks (size={}, overlap={})",
                length, chunks.size(), chunkSize, overlap);
        return chunks;
    }

    public int estimateTokenCount(String text) {
        // Rough approximation: ~4 characters per token for English text
        return Math.max(1, text.length() / 4);
    }

    private int lastSentenceBreak(String text, int start, int end) {
        int best = -1;
        for (int i = end - 1; i > start + 1; i--) {
            char c = text.charAt(i);
            if ((c == '.' || c == '!' || c == '?') && i + 1 < text.length() && text.charAt(i + 1) == ' ') {
                best = i + 1;
                break;
            }
        }
        return best;
    }
}
