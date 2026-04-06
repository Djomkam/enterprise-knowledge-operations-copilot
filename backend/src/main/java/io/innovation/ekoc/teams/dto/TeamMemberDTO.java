package io.innovation.ekoc.teams.dto;

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
public class TeamMemberDTO {
    private UUID userId;
    private String username;
    private String fullName;
    private String role;
    private boolean active;
    private Instant joinedAt;
}
