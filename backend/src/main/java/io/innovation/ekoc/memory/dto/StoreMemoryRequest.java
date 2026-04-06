package io.innovation.ekoc.memory.dto;

import io.innovation.ekoc.memory.domain.MemoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StoreMemoryRequest {

    @NotNull(message = "Memory type is required")
    private MemoryType type;

    @NotBlank(message = "Content is required")
    private String content;

    private String metadata;
}
