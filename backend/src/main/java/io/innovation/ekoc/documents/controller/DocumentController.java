package io.innovation.ekoc.documents.controller;

import io.innovation.ekoc.documents.dto.AssignTeamRequest;
import io.innovation.ekoc.documents.dto.DocumentDTO;
import io.innovation.ekoc.documents.dto.UploadDocumentRequest;
import io.innovation.ekoc.shared.dto.ApiResponse;
import io.innovation.ekoc.shared.dto.PagedResponse;
import io.innovation.ekoc.shared.exception.UnauthorizedException;
import io.innovation.ekoc.shared.util.SecurityUtil;
import io.innovation.ekoc.documents.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Document management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(value = {"/upload", ""}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a document for ingestion")
    public ResponseEntity<ApiResponse<DocumentDTO>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart("title") String title,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart(value = "teamId", required = false) String teamId) {
        String username = currentUser();
        UploadDocumentRequest request = new UploadDocumentRequest(
                title,
                description,
                teamId != null ? UUID.fromString(teamId) : null);
        DocumentDTO doc = documentService.upload(file, request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Document uploaded", doc));
    }

    @GetMapping
    @Operation(summary = "List documents accessible to the current user")
    public ResponseEntity<ApiResponse<PagedResponse<DocumentDTO>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID teamId) {
        String username = currentUser();
        var docs = documentService.listDocuments(username, teamId,
                PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(docs)));
    }

    @GetMapping("/{documentId}")
    @Operation(summary = "Get a document by ID")
    public ResponseEntity<ApiResponse<DocumentDTO>> get(@PathVariable UUID documentId) {
        return ResponseEntity.ok(ApiResponse.success(
                documentService.getDocument(documentId, currentUser())));
    }

    @DeleteMapping("/{documentId}")
    @Operation(summary = "Delete a document and its chunks")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID documentId) {
        documentService.deleteDocument(documentId, currentUser());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/{documentId}/team")
    @Operation(summary = "Assign or unassign a document to a team")
    public ResponseEntity<ApiResponse<DocumentDTO>> assignTeam(
            @PathVariable UUID documentId,
            @RequestBody AssignTeamRequest body) {
        DocumentDTO doc = documentService.assignToTeam(documentId, body.getTeamId(), currentUser());
        return ResponseEntity.ok(ApiResponse.success(doc));
    }

    @GetMapping("/{documentId}/versions")
    @Operation(summary = "Get version history for a document")
    public ResponseEntity<ApiResponse<java.util.List<DocumentDTO>>> versions(@PathVariable UUID documentId) {
        return ResponseEntity.ok(ApiResponse.success(
                documentService.getVersionHistory(documentId, currentUser())));
    }

    private String currentUser() {
        return SecurityUtil.getCurrentUsername()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
    }
}
