package io.innovation.ekoc.admin.controller;

import io.innovation.ekoc.audit.domain.AuditAction;
import io.innovation.ekoc.audit.dto.AuditEventDTO;
import io.innovation.ekoc.audit.service.AuditService;
import io.innovation.ekoc.shared.dto.ApiResponse;
import io.innovation.ekoc.shared.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin-only endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class AdminController {

    private final AuditService auditService;

    @GetMapping("/audit")
    @Operation(summary = "Query all audit events (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<AuditEventDTO>>> getAllAuditEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var events = auditService.queryAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(events)));
    }

    @GetMapping("/audit/user/{userId}")
    @Operation(summary = "Query audit events for a specific user")
    public ResponseEntity<ApiResponse<PagedResponse<AuditEventDTO>>> getAuditByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var events = auditService.queryByUser(userId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(events)));
    }

    @GetMapping("/audit/action/{action}")
    @Operation(summary = "Query audit events by action type")
    public ResponseEntity<ApiResponse<PagedResponse<AuditEventDTO>>> getAuditByAction(
            @PathVariable AuditAction action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var events = auditService.queryByAction(action,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(events)));
    }

    @GetMapping("/audit/range")
    @Operation(summary = "Query audit events within a time range")
    public ResponseEntity<ApiResponse<PagedResponse<AuditEventDTO>>> getAuditByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var events = auditService.queryByTimeRange(from, to,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(events)));
    }
}
