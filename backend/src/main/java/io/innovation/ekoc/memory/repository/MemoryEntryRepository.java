package io.innovation.ekoc.memory.repository;

import io.innovation.ekoc.memory.domain.MemoryEntry;
import io.innovation.ekoc.memory.domain.MemoryType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MemoryEntryRepository extends JpaRepository<MemoryEntry, UUID> {

    List<MemoryEntry> findByUserIdAndTypeAndActiveTrueOrderByCreatedAtDesc(
            UUID userId, MemoryType type, Pageable pageable);

    List<MemoryEntry> findByUserIdAndActiveTrueOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Modifying
    @Query("UPDATE MemoryEntry m SET m.active = false " +
           "WHERE m.user.id = :userId AND m.type = :type AND m.createdAt < :before")
    int deactivateBefore(@Param("userId") UUID userId,
                         @Param("type") MemoryType type,
                         @Param("before") Instant before);
}
