package io.innovation.ekoc.analytics.controller;

import io.innovation.ekoc.analytics.dto.AnalyticsSummaryDTO;
import io.innovation.ekoc.analytics.dto.QueryAnalyticDTO;
import io.innovation.ekoc.analytics.service.AnalyticsService;
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
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Analytics", description = "Query analytics (admin only)")
@SecurityRequirement(name = "bearer-jwt")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    @Operation(summary = "List all query analytics (admin)")
    public ResponseEntity<ApiResponse<PagedResponse<QueryAnalyticDTO>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var result = analyticsService.listAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(result)));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "List analytics for a specific user")
    public ResponseEntity<ApiResponse<PagedResponse<QueryAnalyticDTO>>> byUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var result = analyticsService.listByUser(userId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(result)));
    }

    @GetMapping("/team/{teamId}")
    @Operation(summary = "List analytics for a specific team")
    public ResponseEntity<ApiResponse<PagedResponse<QueryAnalyticDTO>>> byTeam(
            @PathVariable UUID teamId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var result = analyticsService.listByTeam(teamId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(result)));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get aggregate analytics summary for a time range (default: last 24 h)")
    public ResponseEntity<ApiResponse<AnalyticsSummaryDTO>> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Instant effectiveTo = to != null ? to : Instant.now();
        Instant effectiveFrom = from != null ? from : effectiveTo.minus(24, ChronoUnit.HOURS);
        return ResponseEntity.ok(ApiResponse.success(analyticsService.summary(effectiveFrom, effectiveTo)));
    }
}
