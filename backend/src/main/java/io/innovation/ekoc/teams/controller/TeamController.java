package io.innovation.ekoc.teams.controller;

import io.innovation.ekoc.shared.dto.ApiResponse;
import io.innovation.ekoc.shared.dto.PagedResponse;
import io.innovation.ekoc.shared.exception.UnauthorizedException;
import io.innovation.ekoc.shared.util.SecurityUtil;
import io.innovation.ekoc.teams.dto.AddMemberRequest;
import io.innovation.ekoc.teams.dto.CreateTeamRequest;
import io.innovation.ekoc.teams.dto.TeamDTO;
import io.innovation.ekoc.teams.dto.TeamMemberDTO;
import io.innovation.ekoc.teams.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
@Tag(name = "Teams", description = "Team management endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    @Operation(summary = "Create a new team")
    public ResponseEntity<ApiResponse<TeamDTO>> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        String username = currentUser();
        TeamDTO team = teamService.createTeam(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Team created", team));
    }

    @GetMapping
    @Operation(summary = "List all active teams")
    public ResponseEntity<ApiResponse<PagedResponse<TeamDTO>>> listTeams(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var teams = teamService.listTeams(PageRequest.of(page, size, Sort.by("name").ascending()));
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(teams)));
    }

    @GetMapping("/{teamId}")
    @Operation(summary = "Get a team by ID")
    public ResponseEntity<ApiResponse<TeamDTO>> getTeam(@PathVariable UUID teamId) {
        return ResponseEntity.ok(ApiResponse.success(teamService.getTeam(teamId)));
    }

    @GetMapping("/my")
    @Operation(summary = "List teams the current user belongs to")
    public ResponseEntity<ApiResponse<List<TeamDTO>>> myTeams() {
        return ResponseEntity.ok(ApiResponse.success(teamService.getTeamsForUser(currentUser())));
    }

    @GetMapping("/{teamId}/members")
    @Operation(summary = "List members of a team")
    public ResponseEntity<ApiResponse<List<TeamMemberDTO>>> listMembers(@PathVariable UUID teamId) {
        return ResponseEntity.ok(ApiResponse.success(teamService.listMembers(teamId)));
    }

    @PostMapping("/{teamId}/members")
    @Operation(summary = "Add a member to a team")
    public ResponseEntity<ApiResponse<Void>> addMember(
            @PathVariable UUID teamId,
            @Valid @RequestBody AddMemberRequest request) {
        teamService.addMember(teamId, request, currentUser());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    @Operation(summary = "Remove a member from a team")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable UUID teamId,
            @PathVariable UUID userId) {
        teamService.removeMember(teamId, userId, currentUser());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private String currentUser() {
        return SecurityUtil.getCurrentUsername()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
    }
}
