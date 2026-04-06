package io.innovation.ekoc.memory.controller;

import io.innovation.ekoc.memory.domain.MemoryType;
import io.innovation.ekoc.memory.dto.MemoryEntryDTO;
import io.innovation.ekoc.memory.dto.StoreMemoryRequest;
import io.innovation.ekoc.memory.service.MemoryService;
import io.innovation.ekoc.shared.dto.ApiResponse;
import io.innovation.ekoc.shared.exception.UnauthorizedException;
import io.innovation.ekoc.shared.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/memory")
@RequiredArgsConstructor
@Tag(name = "Memory", description = "User memory management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class MemoryController {

    private final MemoryService memoryService;

    @PostMapping
    @Operation(summary = "Store a memory entry for the current user")
    public ResponseEntity<ApiResponse<MemoryEntryDTO>> store(@Valid @RequestBody StoreMemoryRequest request) {
        MemoryEntryDTO entry = memoryService.store(request, currentUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Memory stored", entry));
    }

    @GetMapping
    @Operation(summary = "Get recent memory entries")
    public ResponseEntity<ApiResponse<List<MemoryEntryDTO>>> getRecent(
            @RequestParam(required = false) MemoryType type,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                memoryService.getRecent(currentUser(), type, limit)));
    }

    @GetMapping("/search")
    @Operation(summary = "Search memories by semantic similarity")
    public ResponseEntity<ApiResponse<List<MemoryEntryDTO>>> search(
            @RequestParam String query,
            @RequestParam(required = false) MemoryType type,
            @RequestParam(defaultValue = "5") int topK) {
        return ResponseEntity.ok(ApiResponse.success(
                memoryService.searchRelevant(query, currentUser(), type, topK)));
    }

    @DeleteMapping("/{memoryId}")
    @Operation(summary = "Deactivate (soft-delete) a memory entry")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID memoryId) {
        memoryService.deactivate(memoryId, currentUser());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private String currentUser() {
        return SecurityUtil.getCurrentUsername()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
    }
}
