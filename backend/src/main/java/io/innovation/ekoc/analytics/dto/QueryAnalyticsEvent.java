package io.innovation.ekoc.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryAnalyticsEvent {
    private UUID userId;
    private UUID teamId;
    private UUID conversationId;
    private String query;
    private Integer responseLength;
    private Integer retrievalHits;
    private Long latencyMs;
    private Integer tokensUsed;
    private String modelUsed;
    private boolean success;
    private String errorMessage;
}
