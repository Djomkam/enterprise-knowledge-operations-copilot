package io.innovation.ekoc.prompttemplate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplateDTO {
    private UUID id;
    private String name;
    private String description;
    private String systemPrompt;
    private String roleType;
    private UUID teamId;
    private String teamName;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
