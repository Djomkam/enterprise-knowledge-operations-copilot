package io.innovation.ekoc.documents.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class AssignTeamRequest {
    private UUID teamId;
}
