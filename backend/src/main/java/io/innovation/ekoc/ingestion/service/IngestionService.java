package io.innovation.ekoc.ingestion.service;

import com.pgvector.PGvector;
import io.innovation.ekoc.ai.service.EmbeddingService;
import io.innovation.ekoc.config.AIConfig;
import io.innovation.ekoc.config.MinIOConfig;
import io.innovation.ekoc.documents.domain.Document;
import io.innovation.ekoc.documents.domain.DocumentChunk;
import io.innovation.ekoc.documents.domain.DocumentStatus;
import io.innovation.ekoc.documents.repository.DocumentChunkRepository;
import io.innovation.ekoc.documents.repository.DocumentRepository;
import io.innovation.ekoc.ingestion.dto.IngestionEvent;
import io.innovation.ekoc.ingestion.processor.DocumentProcessor;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final ChunkingService chunkingService;
    private final MinioClient minioClient;
    private final MinIOConfig minIOConfig;
    private final AIConfig aiConfig;
    private final List<DocumentProcessor> processors;

    @Transactional
    public void ingest(IngestionEvent event) {
        UUID documentId = event.getDocumentId();
        log.info("Starting ingestion for document {}", documentId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        document.setStatus(DocumentStatus.PROCESSING);
        documentRepository.save(document);

        try {
            String rawText = downloadAndExtract(event);
            List<String> chunks = chunkingService.chunk(rawText);
            log.info("Document {} split into {} chunks", documentId, chunks.size());

            embedAndPersistChunks(document, chunks);

            document.setStatus(DocumentStatus.COMPLETED);
            document.setChunkCount(chunks.size());
            documentRepository.save(document);
            log.info("Ingestion complete for document {} ({} chunks)", documentId, chunks.size());

        } catch (Exception e) {
            log.error("Ingestion failed for document {}: {}", documentId, e.getMessage(), e);
            document.setStatus(DocumentStatus.FAILED);
            document.setErrorMessage(truncate(e.getMessage(), 1900));
            documentRepository.save(document);
            throw new RuntimeException("Ingestion failed for document " + documentId, e);
        }
    }

    private String downloadAndExtract(IngestionEvent event) throws Exception {
        DocumentProcessor processor = processors.stream()
                .filter(p -> p.supports(event.getContentType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No processor for content type: " + event.getContentType()));

        log.debug("Downloading {} from MinIO key {}", event.getFileName(), event.getStorageKey());
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minIOConfig.getBucketName())
                        .object(event.getStorageKey())
                        .build())) {
            return processor.extract(stream);
        }
    }

    private void embedAndPersistChunks(Document document, List<String> chunkTexts) {
        int batchSize = aiConfig.getEmbeddings().getBatchSize();
        List<DocumentChunk> chunksToSave = new ArrayList<>();

        for (int batchStart = 0; batchStart < chunkTexts.size(); batchStart += batchSize) {
            int batchEnd = Math.min(batchStart + batchSize, chunkTexts.size());
            List<String> batch = chunkTexts.subList(batchStart, batchEnd);

            log.debug("Embedding batch {}-{} of {}", batchStart, batchEnd, chunkTexts.size());
            List<float[]> embeddings = embeddingService.embedBatch(batch);

            for (int i = 0; i < batch.size(); i++) {
                String chunkText = batch.get(i);
                float[] embedding = embeddings.get(i);

                DocumentChunk chunk = DocumentChunk.builder()
                        .document(document)
                        .position(batchStart + i)
                        .content(chunkText)
                        .tokenCount(chunkingService.estimateTokenCount(chunkText))
                        .embedding(new PGvector(embedding))
                        .build();

                chunksToSave.add(chunk);
            }

            chunkRepository.saveAll(chunksToSave);
            chunksToSave.clear();
        }
    }

    private String truncate(String message, int maxLength) {
        if (message == null) return "Unknown error";
        return message.length() > maxLength ? message.substring(0, maxLength) : message;
    }
}
