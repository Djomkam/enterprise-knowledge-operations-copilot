package io.innovation.ekoc.audit.dto;

import io.innovation.ekoc.audit.domain.AuditAction;
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
public class AuditEventDTO {
    private UUID id;
    private UUID userId;
    private String username;
    private AuditAction action;
    private String resource;
    private String resourceId;
    private String details;
    private String ipAddress;
    private boolean success;
    private String errorMessage;
    private Instant createdAt;
}
