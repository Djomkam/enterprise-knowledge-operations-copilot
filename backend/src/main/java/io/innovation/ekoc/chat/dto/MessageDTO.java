package io.innovation.ekoc.chat.dto;

import io.innovation.ekoc.chat.domain.Message;
import io.innovation.ekoc.chat.domain.MessageRole;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class MessageDTO {
    private UUID id;
    private MessageRole role;
    private String content;
    private String citations;
    private Integer tokensUsed;
    private Instant createdAt;

    public static MessageDTO from(Message m) {
        return MessageDTO.builder()
                .id(m.getId())
                .role(m.getRole())
                .content(m.getContent())
                .citations(m.getCitations())
                .tokensUsed(m.getTokensUsed())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
