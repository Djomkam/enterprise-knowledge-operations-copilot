package io.innovation.ekoc.analytics.consumer;

import io.innovation.ekoc.analytics.domain.QueryAnalytic;
import io.innovation.ekoc.analytics.dto.QueryAnalyticsEvent;
import io.innovation.ekoc.analytics.repository.QueryAnalyticRepository;
import io.innovation.ekoc.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsConsumer {

    private final QueryAnalyticRepository repository;

    @RabbitListener(queues = RabbitMQConfig.ANALYTICS_QUEUE)
    @Transactional
    public void consume(QueryAnalyticsEvent event) {
        try {
            repository.save(QueryAnalytic.builder()
                    .userId(event.getUserId())
                    .teamId(event.getTeamId())
                    .conversationId(event.getConversationId())
                    .query(event.getQuery())
                    .responseLength(event.getResponseLength())
                    .retrievalHits(event.getRetrievalHits() != null ? event.getRetrievalHits() : 0)
                    .latencyMs(event.getLatencyMs())
                    .tokensUsed(event.getTokensUsed())
                    .modelUsed(event.getModelUsed())
                    .success(event.isSuccess())
                    .errorMessage(event.getErrorMessage())
                    .build());
            log.debug("Persisted analytics event for userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to persist analytics event: {}", e.getMessage());
        }
    }
}
