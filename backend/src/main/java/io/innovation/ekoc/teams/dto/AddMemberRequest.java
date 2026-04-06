package io.innovation.ekoc.teams.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddMemberRequest {

    @NotBlank(message = "Username is required")
    private String username;

    private String role = "MEMBER"; // OWNER, MEMBER, VIEWER
}
