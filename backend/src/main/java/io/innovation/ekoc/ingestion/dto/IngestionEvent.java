package io.innovation.ekoc.ingestion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionEvent {
    private UUID documentId;
    private String storageKey;
    private String contentType;
    private String fileName;
}
