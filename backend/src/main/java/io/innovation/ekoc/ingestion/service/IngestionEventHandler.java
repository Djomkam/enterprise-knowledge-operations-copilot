package io.innovation.ekoc.ingestion.service;

import io.innovation.ekoc.config.RabbitMQConfig;
import io.innovation.ekoc.ingestion.dto.IngestionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class IngestionEventHandler {

    private final IngestionService ingestionService;

    @RabbitListener(queues = RabbitMQConfig.INGESTION_QUEUE)
    public void handleIngestionEvent(IngestionEvent event) {
        log.info("Received ingestion event for document {} ({})", event.getDocumentId(), event.getFileName());
        try {
            ingestionService.ingest(event);
        } catch (Exception e) {
            log.error("Failed to process ingestion event for document {}: {}",
                    event.getDocumentId(), e.getMessage());
            // Do not re-throw — the document status is already set to FAILED by IngestionService.
            // Dead-lettering can be configured at the queue level for persistent failures.
        }
    }
}
