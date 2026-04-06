package io.innovation.ekoc.teams.service;

import io.innovation.ekoc.shared.exception.BusinessException;
import io.innovation.ekoc.shared.exception.ResourceNotFoundException;
import io.innovation.ekoc.teams.domain.Team;
import io.innovation.ekoc.teams.domain.TeamMember;
import io.innovation.ekoc.teams.dto.AddMemberRequest;
import io.innovation.ekoc.teams.dto.CreateTeamRequest;
import io.innovation.ekoc.teams.dto.TeamDTO;
import io.innovation.ekoc.teams.dto.TeamMemberDTO;
import io.innovation.ekoc.teams.repository.TeamMemberRepository;
import io.innovation.ekoc.teams.repository.TeamRepository;
import io.innovation.ekoc.users.domain.User;
import io.innovation.ekoc.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserService userService;

    @Transactional
    public TeamDTO createTeam(CreateTeamRequest request, String creatorUsername) {
        if (teamRepository.existsByName(request.getName())) {
            throw new BusinessException("Team name already exists: " + request.getName());
        }
        Team team = Team.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        team = teamRepository.save(team);

        // Auto-add creator as OWNER
        User creator = userService.findByUsername(creatorUsername);
        TeamMember ownerMembership = TeamMember.builder()
                .team(team)
                .user(creator)
                .role("OWNER")
                .build();
        teamMemberRepository.save(ownerMembership);

        log.info("Team '{}' created by {}", team.getName(), creatorUsername);
        return toDTO(team);
    }

    @Transactional(readOnly = true)
    public TeamDTO getTeam(UUID teamId) {
        return toDTO(findTeamById(teamId));
    }

    @Transactional(readOnly = true)
    public Page<TeamDTO> listTeams(Pageable pageable) {
        return teamRepository.findByActiveTrue(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<TeamDTO> getTeamsForUser(String username) {
        User user = userService.findByUsername(username);
        return teamMemberRepository.findByUserIdAndActiveTrue(user.getId()).stream()
                .map(tm -> toDTO(tm.getTeam()))
                .toList();
    }

    @Transactional
    public void addMember(UUID teamId, AddMemberRequest request, String requestingUsername) {
        Team team = findTeamById(teamId);
        requireOwnerOrAdmin(teamId, requestingUsername);

        User newMember = userService.findByUsername(request.getUsername());
        if (teamMemberRepository.existsByTeamIdAndUserIdAndActiveTrue(teamId, newMember.getId())) {
            throw new BusinessException("User is already a member of this team");
        }

        TeamMember membership = TeamMember.builder()
                .team(team)
                .user(newMember)
                .role(request.getRole() != null ? request.getRole().toUpperCase() : "MEMBER")
                .build();
        teamMemberRepository.save(membership);
        log.info("User {} added to team {} as {}", request.getUsername(), team.getName(), membership.getRole());
    }

    @Transactional
    public void removeMember(UUID teamId, UUID userId, String requestingUsername) {
        requireOwnerOrAdmin(teamId, requestingUsername);

        TeamMember membership = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Team membership not found"));
        membership.setActive(false);
        teamMemberRepository.save(membership);
        log.info("User {} removed from team {}", userId, teamId);
    }

    @Transactional(readOnly = true)
    public List<TeamMemberDTO> listMembers(UUID teamId) {
        findTeamById(teamId); // verify team exists
        return teamMemberRepository.findByTeamIdAndActiveTrue(teamId).stream()
                .map(this::toMemberDTO)
                .toList();
    }

    private Team findTeamById(UUID teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));
    }

    private void requireOwnerOrAdmin(UUID teamId, String username) {
        User user = userService.findByUsername(username);
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) return;

        teamMemberRepository.findByTeamIdAndUserId(teamId, user.getId())
                .filter(tm -> tm.isActive() && "OWNER".equals(tm.getRole()))
                .orElseThrow(() -> new AccessDeniedException("Only team owners or admins can perform this action"));
    }

    private TeamDTO toDTO(Team team) {
        return TeamDTO.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .active(team.isActive())
                .createdAt(team.getCreatedAt())
                .build();
    }

    private TeamMemberDTO toMemberDTO(TeamMember tm) {
        return TeamMemberDTO.builder()
                .userId(tm.getUser().getId())
                .username(tm.getUser().getUsername())
                .fullName(tm.getUser().getFullName())
                .role(tm.getRole())
                .active(tm.isActive())
                .joinedAt(tm.getCreatedAt())
                .build();
    }
}
