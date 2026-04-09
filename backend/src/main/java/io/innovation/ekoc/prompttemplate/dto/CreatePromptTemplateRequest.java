package io.innovation.ekoc.prompttemplate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreatePromptTemplateRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotBlank
    private String systemPrompt;

    /** Optional: USER, ADMIN, ANALYST. Null = applies to all roles. */
    private String roleType;

    /** Optional: scoped to a specific team. Null = global. */
    private UUID teamId;
}
