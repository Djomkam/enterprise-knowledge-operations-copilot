package io.innovation.ekoc.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummaryDTO {
    private long totalQueries;
    private long successfulQueries;
    private Double avgLatencyMs;
    private double successRate;
}
