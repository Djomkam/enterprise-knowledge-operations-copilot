package io.innovation.ekoc.memory.dto;

import io.innovation.ekoc.memory.domain.MemoryType;
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
public class MemoryEntryDTO {
    private UUID id;
    private MemoryType type;
    private String content;
    private String metadata;
    private Double relevanceScore;
    private boolean active;
    private Instant createdAt;
}
