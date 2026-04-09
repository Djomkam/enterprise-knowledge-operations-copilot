package io.innovation.ekoc.retrieval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {
    private String query;
    private Integer topK;
    private Double similarityThreshold;
    private List<UUID> documentIds;
    private List<UUID> teamIds;
    private UUID userId;
}
