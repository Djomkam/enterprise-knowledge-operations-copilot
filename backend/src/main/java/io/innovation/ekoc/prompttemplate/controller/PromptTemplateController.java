package io.innovation.ekoc.prompttemplate.controller;

import io.innovation.ekoc.prompttemplate.dto.CreatePromptTemplateRequest;
import io.innovation.ekoc.prompttemplate.dto.PromptTemplateDTO;
import io.innovation.ekoc.prompttemplate.service.PromptTemplateService;
import io.innovation.ekoc.shared.dto.ApiResponse;
import io.innovation.ekoc.shared.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/prompt-templates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Prompt Templates", description = "Role/team-based prompt template management (admin only)")
@SecurityRequirement(name = "bearer-jwt")
public class PromptTemplateController {

    private final PromptTemplateService service;

    @GetMapping
    @Operation(summary = "List active prompt templates")
    public ResponseEntity<ApiResponse<PagedResponse<PromptTemplateDTO>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var result = service.listActive(PageRequest.of(page, size, Sort.by("name")));
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(result)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a prompt template by ID")
    public ResponseEntity<ApiResponse<PromptTemplateDTO>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a new prompt template")
    public ResponseEntity<ApiResponse<PromptTemplateDTO>> create(
            @Valid @RequestBody CreatePromptTemplateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Template created", service.create(req)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a prompt template")
    public ResponseEntity<ApiResponse<PromptTemplateDTO>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePromptTemplateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a prompt template")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        service.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
