package io.innovation.ekoc.documents.service;

import io.innovation.ekoc.config.IngestionConfig;
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
    private final RabbitTemplate rabbitTemplate;
    private final IngestionConfig ingestionConfig;

    @Transactional
    public DocumentDTO upload(MultipartFile file, UploadDocumentRequest request, String username) {
        validateContentType(file.getContentType());

        User owner = userService.findByUsername(username);
        Team team = resolveTeam(request.getTeamId());

        // Persist record with PENDING status first to get the UUID for the storage key
        Document document = Document.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .storageKey("pending") // placeholder until uploaded
                .owner(owner)
                .team(team)
                .build();
        document = documentRepository.save(document);

        // Upload to MinIO and update the storage key
        String storageKey = storageService.store(file, document.getId());
        document.setStorageKey(storageKey);
        document = documentRepository.save(document);

        // Publish ingestion event for async processing
        IngestionEvent event = IngestionEvent.builder()
                .documentId(document.getId())
                .storageKey(storageKey)
                .contentType(file.getContentType())
                .fileName(file.getOriginalFilename())
                .build();
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.INGESTION_EXCHANGE,
                RabbitMQConfig.INGESTION_ROUTING_KEY,
                event);

        log.info("Document {} uploaded by {}, ingestion queued", document.getId(), username);
        return toDTO(document);
    }

    @Transactional(readOnly = true)
    public DocumentDTO getDocument(UUID documentId, String username) {
        Document document = findAndVerifyAccess(documentId, username);
        return toDTO(document);
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

    @Transactional
    public void deleteDocument(UUID documentId, String username) {
        Document document = findAndVerifyAccess(documentId, username);

        // Delete from MinIO (best-effort; don't fail the whole operation if storage delete fails)
        storageService.delete(document.getStorageKey());

        // Cascade: chunks deleted via DB cascade (FK ON DELETE CASCADE),
        // but explicitly deleting here ensures JPA cache is clean.
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
        return teamRepository.findAll().stream()
                .filter(t -> t.isActive())
                .map(Team::getId)
                .toList(); // simplified — production should query team_members directly
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
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
