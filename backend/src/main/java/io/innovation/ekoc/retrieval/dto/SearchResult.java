package io.innovation.ekoc.retrieval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {
    private UUID chunkId;
    private UUID documentId;
    private String documentTitle;
    private String content;
    private Integer position;
    private Double similarityScore;
    private String metadata;
}
