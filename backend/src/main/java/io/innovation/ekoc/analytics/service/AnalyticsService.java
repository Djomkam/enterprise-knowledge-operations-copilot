package io.innovation.ekoc.analytics.service;

import io.innovation.ekoc.analytics.domain.QueryAnalytic;
import io.innovation.ekoc.analytics.dto.AnalyticsSummaryDTO;
import io.innovation.ekoc.analytics.dto.QueryAnalyticDTO;
import io.innovation.ekoc.analytics.repository.QueryAnalyticRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final QueryAnalyticRepository repository;

    @Transactional(readOnly = true)
    public Page<QueryAnalyticDTO> listAll(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<QueryAnalyticDTO> listByUser(UUID userId, Pageable pageable) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<QueryAnalyticDTO> listByTeam(UUID teamId, Pageable pageable) {
        return repository.findByTeamIdOrderByCreatedAtDesc(teamId, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<QueryAnalyticDTO> listByRange(Instant from, Instant to, Pageable pageable) {
        return repository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryDTO summary(Instant from, Instant to) {
        long total = repository.countTotalBetween(from, to);
        long success = repository.countSuccessBetween(from, to);
        Double avgLatency = repository.avgLatencyBetween(from, to);
        double rate = total > 0 ? (double) success / total * 100.0 : 0.0;
        return AnalyticsSummaryDTO.builder()
                .totalQueries(total)
                .successfulQueries(success)
                .avgLatencyMs(avgLatency)
                .successRate(rate)
                .build();
    }

    private QueryAnalyticDTO toDTO(QueryAnalytic q) {
        return QueryAnalyticDTO.builder()
                .id(q.getId())
                .userId(q.getUserId())
                .teamId(q.getTeamId())
                .conversationId(q.getConversationId())
                .query(q.getQuery())
                .responseLength(q.getResponseLength())
                .retrievalHits(q.getRetrievalHits())
                .latencyMs(q.getLatencyMs())
                .tokensUsed(q.getTokensUsed())
                .modelUsed(q.getModelUsed())
                .success(q.isSuccess())
                .errorMessage(q.getErrorMessage())
                .createdAt(q.getCreatedAt())
                .build();
    }
}
