package io.innovation.ekoc.chat.repository;

import io.innovation.ekoc.chat.domain.Message;
import io.innovation.ekoc.chat.domain.MessageRole;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    List<Message> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    long countByConversationId(UUID conversationId);

    Optional<Message> findTopByConversationIdAndRoleOrderByCreatedAtDesc(UUID conversationId, MessageRole role);

    List<Message> findByConversationIdAndRoleNotAndCreatedAtBeforeOrderByCreatedAtAsc(
            UUID conversationId, MessageRole excludedRole, Instant before);

    @Modifying
    @Query("DELETE FROM Message m WHERE m.conversation.id = :conversationId AND m.role <> :keepRole AND m.createdAt < :before")
    void deleteOlderThan(@Param("conversationId") UUID conversationId,
                         @Param("keepRole") MessageRole keepRole,
                         @Param("before") Instant before);
}
