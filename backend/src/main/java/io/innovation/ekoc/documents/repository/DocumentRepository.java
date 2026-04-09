package io.innovation.ekoc.documents.repository;

import io.innovation.ekoc.documents.domain.Document;
import io.innovation.ekoc.documents.domain.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Page<Document> findByOwnerId(UUID ownerId, Pageable pageable);

    Page<Document> findByTeamId(UUID teamId, Pageable pageable);

    List<Document> findByStatus(DocumentStatus status);

    Page<Document> findByOwnerIdOrTeamIdIn(UUID ownerId, List<UUID> teamIds, Pageable pageable);

    /** Find the latest version of a document by filename + owner (for re-upload detection). */
    Optional<Document> findTopByFileNameAndOwnerIdOrderByVersionNumberDesc(String fileName, UUID ownerId);

    /** Fetch full version history for a document chain starting from the given root parent. */
    @Query("SELECT d FROM Document d WHERE d.parentId = :parentId OR d.id = :parentId ORDER BY d.versionNumber ASC")
    List<Document> findVersionHistory(@Param("parentId") UUID parentId);
}
