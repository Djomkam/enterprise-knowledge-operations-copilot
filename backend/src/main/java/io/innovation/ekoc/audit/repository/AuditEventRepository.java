package io.innovation.ekoc.audit.repository;

import io.innovation.ekoc.audit.domain.AuditAction;
import io.innovation.ekoc.audit.domain.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    Page<AuditEvent> findByUserId(UUID userId, Pageable pageable);

    Page<AuditEvent> findByAction(AuditAction action, Pageable pageable);

    Page<AuditEvent> findByResourceAndResourceId(String resource, String resourceId, Pageable pageable);

    Page<AuditEvent> findByCreatedAtBetween(Instant from, Instant to, Pageable pageable);
}
