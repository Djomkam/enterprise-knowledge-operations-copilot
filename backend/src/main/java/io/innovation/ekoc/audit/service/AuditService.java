package io.innovation.ekoc.audit.service;

import io.innovation.ekoc.audit.domain.AuditAction;
import io.innovation.ekoc.audit.domain.AuditEvent;
import io.innovation.ekoc.audit.dto.AuditEventDTO;
import io.innovation.ekoc.audit.repository.AuditEventRepository;
import io.innovation.ekoc.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditRepository;
    private final UserRepository userRepository;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String username, AuditAction action, String resource,
                    String resourceId, boolean success, String details, String errorMessage) {
        try {
            var userOpt = userRepository.findByUsername(username);
            AuditEvent event = AuditEvent.builder()
                    .user(userOpt.orElse(null))
                    .action(action)
                    .resource(resource)
                    .resourceId(resourceId)
                    .success(success)
                    .details(details)
                    .errorMessage(errorMessage)
                    .build();
            auditRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to persist audit event action={} user={}: {}", action, username, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditEventDTO> queryAll(Pageable pageable) {
        return auditRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<AuditEventDTO> queryByUser(UUID userId, Pageable pageable) {
        return auditRepository.findByUserId(userId, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<AuditEventDTO> queryByAction(AuditAction action, Pageable pageable) {
        return auditRepository.findByAction(action, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<AuditEventDTO> queryByTimeRange(Instant from, Instant to, Pageable pageable) {
        return auditRepository.findByCreatedAtBetween(from, to, pageable).map(this::toDTO);
    }

    private AuditEventDTO toDTO(AuditEvent event) {
        return AuditEventDTO.builder()
                .id(event.getId())
                .userId(event.getUser() != null ? event.getUser().getId() : null)
                .username(event.getUser() != null ? event.getUser().getUsername() : "anonymous")
                .action(event.getAction())
                .resource(event.getResource())
                .resourceId(event.getResourceId())
                .details(event.getDetails())
                .ipAddress(event.getIpAddress())
                .success(event.isSuccess())
                .errorMessage(event.getErrorMessage())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
