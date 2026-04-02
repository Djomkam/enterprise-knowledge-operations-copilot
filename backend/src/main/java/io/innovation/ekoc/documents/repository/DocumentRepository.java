package io.innovation.ekoc.documents.repository;

import io.innovation.ekoc.documents.domain.Document;
import io.innovation.ekoc.documents.domain.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Page<Document> findByOwnerId(UUID ownerId, Pageable pageable);

    Page<Document> findByTeamId(UUID teamId, Pageable pageable);

    List<Document> findByStatus(DocumentStatus status);

    Page<Document> findByOwnerIdOrTeamIdIn(UUID ownerId, List<UUID> teamIds, Pageable pageable);
}
