package io.innovation.ekoc.documents.service;

import io.innovation.ekoc.audit.annotation.Auditable;
import io.innovation.ekoc.audit.domain.AuditAction;
import io.innovation.ekoc.config.IngestionConfig;
import org.springframework.security.access.prepost.PreAuthorize;
import io.innovation.ekoc.config.RabbitMQConfig;
import io.innovation.ekoc.documents.domain.Document;
import io.innovation.ekoc.documents.domain.DocumentStatus;
import io.innovation.ekoc.documents.dto.DocumentDTO;
import io.innovation.ekoc.documents.dto.UploadDocumentRequest;
import io.innovation.ekoc.documents.repository.DocumentChunkRepository;
import io.innovation.ekoc.documents.repository.DocumentRepository;
import io.innovation.ekoc.ingestion.dto.IngestionEvent;
import io.innovation.ekoc.shared.exception.BusinessException;
import io.innovation.ekoc.shared.exception.ResourceNotFoundException;
import io.innovation.ekoc.teams.domain.Team;
import io.innovation.ekoc.teams.repository.TeamMemberRepository;
import io.innovation.ekoc.teams.repository.TeamRepository;
import io.innovation.ekoc.users.domain.User;
import io.innovation.ekoc.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentStorageService storageService;
    private final UserService userService;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RabbitTemplate rabbitTemplate;
    private final IngestionConfig ingestionConfig;

    @Auditable(action = AuditAction.DOCUMENT_UPLOAD, resource = "Document")
    @Transactional
    public DocumentDTO upload(MultipartFile file, UploadDocumentRequest request, String username) {
        validateContentType(file.getContentType());

        User owner = userService.findByUsername(username);
        Team team = resolveTeam(request.getTeamId());

        // Detect re-upload: same filename by same owner → create a new version
        String fileName = file.getOriginalFilename();
        int nextVersion = 1;
        UUID parentId = null;
        var existing = documentRepository.findTopByFileNameAndOwnerIdOrderByVersionNumberDesc(fileName, owner.getId());
        if (existing.isPresent()) {
            Document prev = existing.get();
            parentId = prev.getParentId() != null ? prev.getParentId() : prev.getId();
            nextVersion = prev.getVersionNumber() + 1;
            log.info("Re-upload of '{}': creating version {} (root={})", fileName, nextVersion, parentId);
        }

        Document document = Document.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .fileName(fileName)
                .contentType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .storageKey("pending")
                .owner(owner)
                .team(team)
                .parentId(parentId)
                .versionNumber(nextVersion)
                .build();
        document = documentRepository.save(document);

        String storageKey = storageService.store(file, document.getId());
        document.setStorageKey(storageKey);
        document = documentRepository.save(document);

        IngestionEvent event = IngestionEvent.builder()
                .documentId(document.getId())
                .storageKey(storageKey)
                .contentType(file.getContentType())
                .fileName(fileName)
                .build();
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.INGESTION_EXCHANGE,
                RabbitMQConfig.INGESTION_ROUTING_KEY,
                event);

        log.info("Document {} v{} uploaded by {}, ingestion queued", document.getId(), nextVersion, username);
        return toDTO(document);
    }

    @PreAuthorize("@documentAcl.canRead(authentication, #documentId)")
    @Transactional(readOnly = true)
    public DocumentDTO getDocument(UUID documentId, String username) {
        Document document = findAndVerifyAccess(documentId, username);
        return toDTO(document);
    }

    /** Returns all versions (oldest first) of the document chain containing {@code documentId}. */
    @PreAuthorize("@documentAcl.canRead(authentication, #documentId)")
    @Transactional(readOnly = true)
    public List<DocumentDTO> getVersionHistory(UUID documentId, String username) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));
        UUID rootId = doc.getParentId() != null ? doc.getParentId() : doc.getId();
        return documentRepository.findVersionHistory(rootId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public Page<DocumentDTO> listDocuments(String username, UUID teamId, Pageable pageable) {
        User user = userService.findByUsername(username);
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        Page<Document> documents;
        if (isAdmin) {
            documents = documentRepository.findAll(pageable);
        } else if (teamId != null) {
            documents = documentRepository.findByTeamId(teamId, pageable);
        } else {
            List<UUID> teamIds = getTeamIds(user);
            documents = documentRepository.findByOwnerIdOrTeamIdIn(user.getId(), teamIds, pageable);
        }
        return documents.map(this::toDTO);
    }

    @PreAuthorize("@documentAcl.canDelete(authentication, #documentId)")
    @Auditable(action = AuditAction.DOCUMENT_DELETE, resource = "Document")
    @Transactional
    public void deleteDocument(UUID documentId, String username) {
        Document document = findAndVerifyAccess(documentId, username);
        storageService.delete(document.getStorageKey());
        chunkRepository.deleteByDocumentId(documentId);
        documentRepository.delete(document);
        log.info("Document {} deleted by {}", documentId, username);
    }

    @Transactional
    public DocumentDTO assignToTeam(UUID documentId, UUID teamId, String username) {
        Document document = findAndVerifyAccess(documentId, username);
        Team team = teamId != null
                ? teamRepository.findById(teamId)
                        .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId))
                : null;
        document.setTeam(team);
        return toDTO(documentRepository.save(document));
    }

    private void validateContentType(String contentType) {
        if (contentType == null || ingestionConfig.getSupportedTypes() == null) return;
        if (!ingestionConfig.getSupportedTypes().contains(contentType)) {
            throw new BusinessException("Unsupported file type: " + contentType +
                    ". Supported: " + ingestionConfig.getSupportedTypes());
        }
    }

    private Team resolveTeam(UUID teamId) {
        if (teamId == null) return null;
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));
    }

    private Document findAndVerifyAccess(UUID documentId, String username) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", documentId));
        User user = userService.findByUsername(username);
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = document.getOwner().getId().equals(user.getId());
        boolean inTeam = document.getTeam() != null &&
                getTeamIds(user).contains(document.getTeam().getId());
        if (!isAdmin && !isOwner && !inTeam) {
            throw new AccessDeniedException("Access denied to document: " + documentId);
        }
        return document;
    }

    private List<UUID> getTeamIds(User user) {
        return teamMemberRepository.findByUserIdAndActiveTrue(user.getId()).stream()
                .map(tm -> tm.getTeam().getId())
                .toList();
    }

    private DocumentDTO toDTO(Document doc) {
        return DocumentDTO.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .fileName(doc.getFileName())
                .contentType(doc.getContentType())
                .fileSizeBytes(doc.getFileSizeBytes())
                .status(doc.getStatus())
                .ownerId(doc.getOwner().getId())
                .ownerUsername(doc.getOwner().getUsername())
                .teamId(doc.getTeam() != null ? doc.getTeam().getId() : null)
                .teamName(doc.getTeam() != null ? doc.getTeam().getName() : null)
                .chunkCount(doc.getChunkCount())
                .parentId(doc.getParentId())
                .versionNumber(doc.getVersionNumber())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
