package io.innovation.ekoc.chat.dto;

import io.innovation.ekoc.chat.domain.Conversation;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ConversationDTO {
    private UUID id;
    private String title;
    private Integer messageCount;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public static ConversationDTO from(Conversation c) {
        return ConversationDTO.builder()
                .id(c.getId())
                .title(c.getTitle())
                .messageCount(c.getMessageCount())
                .active(c.isActive())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
