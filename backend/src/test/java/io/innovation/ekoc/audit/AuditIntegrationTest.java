package io.innovation.ekoc.audit;

import io.innovation.ekoc.BaseIntegrationTest;
import io.innovation.ekoc.audit.domain.AuditAction;
import io.innovation.ekoc.audit.repository.AuditEventRepository;
import io.innovation.ekoc.auth.dto.LoginRequest;
import io.innovation.ekoc.auth.dto.RegisterRequest;
import io.innovation.ekoc.teams.dto.CreateTeamRequest;
import io.innovation.ekoc.teams.dto.TeamDTO;
import io.innovation.ekoc.shared.dto.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class AuditIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AuditEventRepository auditEventRepository;

    private String token;
    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = String.valueOf(System.nanoTime());
        token = registerAndLogin("audit_test_user_" + suffix, "password123");
    }

    @Test
    void login_createsAuditEvent() {
        // Register a fresh user, then explicitly login to trigger the LOGIN audit event.
        // registerAndLogin() returns early on successful registration without hitting /login.
        String loginUser = "audit_login_user_" + suffix;
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername(loginUser);
        reg.setEmail(loginUser + "@test.com");
        reg.setPassword("password123");
        reg.setFullName("Login Audit User");
        restTemplate.postForEntity("/api/v1/auth/register", reg, Object.class);

        LoginRequest login = new LoginRequest();
        login.setUsername(loginUser);
        login.setPassword("password123");
        restTemplate.postForEntity("/api/v1/auth/login", login, Object.class);

        long count = auditEventRepository.findByAction(
                AuditAction.LOGIN, PageRequest.of(0, 100)).getTotalElements();
        assertThat(count).isGreaterThan(0);
    }

    @Test
    void register_createsAuditEvent() {
        long count = auditEventRepository.findByAction(
                AuditAction.REGISTER, PageRequest.of(0, 100)).getTotalElements();
        assertThat(count).isGreaterThan(0);
    }

    @Test
    void createTeam_createsAuditEvent() {
        CreateTeamRequest req = new CreateTeamRequest();
        req.setName("AuditTeam_" + suffix);

        restTemplate.exchange("/api/v1/teams", HttpMethod.POST,
                new HttpEntity<>(req, bearerHeaders(token)),
                new ParameterizedTypeReference<ApiResponse<TeamDTO>>() {});

        long count = auditEventRepository.findByAction(
                AuditAction.TEAM_CREATE, PageRequest.of(0, 100)).getTotalElements();
        assertThat(count).isGreaterThan(0);
    }
}
