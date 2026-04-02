package io.innovation.ekoc.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private UUID conversationId;
    private UUID messageId;
    private String content;
    private List<Citation> citations;
    private Integer tokensUsed;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Citation {
        private UUID documentId;
        private String documentTitle;
        private String snippet;
        private Double relevanceScore;
    }
}
