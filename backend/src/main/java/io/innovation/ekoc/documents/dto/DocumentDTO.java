package io.innovation.ekoc.documents.dto;

import io.innovation.ekoc.documents.domain.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO {
    private UUID id;
    private String title;
    private String description;
    private String fileName;
    private String contentType;
    private Long fileSizeBytes;
    private DocumentStatus status;
    private UUID ownerId;
    private String ownerUsername;
    private UUID teamId;
    private String teamName;
    private Integer chunkCount;
    private UUID parentId;
    private Integer versionNumber;
    private Instant createdAt;
    private Instant updatedAt;
}
