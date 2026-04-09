package io.innovation.ekoc.teams;

import io.innovation.ekoc.BaseIntegrationTest;
import io.innovation.ekoc.shared.dto.ApiResponse;
import io.innovation.ekoc.teams.dto.AddMemberRequest;
import io.innovation.ekoc.teams.dto.CreateTeamRequest;
import io.innovation.ekoc.teams.dto.TeamDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TeamMembershipIntegrationTest extends BaseIntegrationTest {

    private String ownerToken;
    private String memberToken;
    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = String.valueOf(System.nanoTime());
        ownerToken = registerAndLogin("team_owner_" + suffix, "password123");
        memberToken = registerAndLogin("team_member_" + suffix, "password123");
    }

    @Test
    void createTeam_success() {
        TeamDTO team = createTeam("TestTeam_" + suffix, ownerToken);
        assertThat(team.getId()).isNotNull();
        assertThat(team.getName()).isEqualTo("TestTeam_" + suffix);
    }

    @Test
    void addMember_success() {
        TeamDTO team = createTeam("AddMemberTeam_" + suffix, ownerToken);

        AddMemberRequest req = new AddMemberRequest();
        req.setUsername("team_member_" + suffix);
        req.setRole("MEMBER");

        ResponseEntity<ApiResponse<Void>> resp = restTemplate.exchange(
                "/api/v1/teams/" + team.getId() + "/members",
                HttpMethod.POST,
                new HttpEntity<>(req, bearerHeaders(ownerToken)),
                new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void myTeams_reflectsMembership() {
        TeamDTO team = createTeam("MyTeamTest_" + suffix, ownerToken);

        AddMemberRequest req = new AddMemberRequest();
        req.setUsername("team_member_" + suffix);

        restTemplate.exchange("/api/v1/teams/" + team.getId() + "/members",
                HttpMethod.POST, new HttpEntity<>(req, bearerHeaders(ownerToken)),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        ResponseEntity<ApiResponse<List<TeamDTO>>> resp = restTemplate.exchange(
                "/api/v1/teams/my",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(memberToken)),
                new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<TeamDTO> teams = resp.getBody().getData();
        assertThat(teams).anyMatch(t -> t.getId().equals(team.getId()));
    }

    @Test
    void removeMember_removesFromMyTeams() {
        TeamDTO team = createTeam("RemoveMemberTeam_" + suffix, ownerToken);

        AddMemberRequest req = new AddMemberRequest();
        req.setUsername("team_member_" + suffix);
        restTemplate.exchange("/api/v1/teams/" + team.getId() + "/members",
                HttpMethod.POST, new HttpEntity<>(req, bearerHeaders(ownerToken)),
                new ParameterizedTypeReference<ApiResponse<Void>>() {});

        // Get member's userId
        ResponseEntity<ApiResponse<List<io.innovation.ekoc.teams.dto.TeamMemberDTO>>> membersResp = restTemplate.exchange(
                "/api/v1/teams/" + team.getId() + "/members",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(ownerToken)),
                new ParameterizedTypeReference<>() {});
        var memberId = membersResp.getBody().getData().stream()
                .filter(m -> m.getUsername().equals("team_member_" + suffix))
                .findFirst().orElseThrow().getUserId();

        restTemplate.exchange("/api/v1/teams/" + team.getId() + "/members/" + memberId,
                HttpMethod.DELETE, new HttpEntity<>(bearerHeaders(ownerToken)), Void.class);

        ResponseEntity<ApiResponse<List<TeamDTO>>> myTeams = restTemplate.exchange(
                "/api/v1/teams/my", HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(memberToken)),
                new ParameterizedTypeReference<>() {});

        assertThat(myTeams.getBody().getData()).noneMatch(t -> t.getId().equals(team.getId()));
    }

    private TeamDTO createTeam(String name, String token) {
        CreateTeamRequest req = new CreateTeamRequest();
        req.setName(name);
        req.setDescription("Test team");

        ResponseEntity<ApiResponse<TeamDTO>> resp = restTemplate.exchange(
                "/api/v1/teams", HttpMethod.POST,
                new HttpEntity<>(req, bearerHeaders(token)),
                new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return resp.getBody().getData();
    }
}
