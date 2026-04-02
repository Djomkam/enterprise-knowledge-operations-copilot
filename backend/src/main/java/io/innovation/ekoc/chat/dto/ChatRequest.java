package io.innovation.ekoc.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    private UUID conversationId;

    @NotBlank(message = "Message is required")
    private String message;

    private Boolean includeContext;
}
