package io.innovation.ekoc;

import io.innovation.ekoc.auth.dto.LoginRequest;
import io.innovation.ekoc.auth.dto.LoginResponse;
import io.innovation.ekoc.auth.dto.RegisterRequest;
import io.innovation.ekoc.config.TestAsyncConfig;
import io.innovation.ekoc.config.TestcontainersConfiguration;
import io.innovation.ekoc.shared.dto.ApiResponse;
import io.innovation.ekoc.users.domain.Role;
import io.innovation.ekoc.users.domain.RoleType;
import io.innovation.ekoc.users.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, TestAsyncConfig.class})
public abstract class BaseIntegrationTest {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected RoleRepository roleRepository;

    @BeforeEach
    void seedRoles() {
        for (RoleType type : RoleType.values()) {
            if (roleRepository.findByName(type).isEmpty()) {
                roleRepository.save(Role.builder().name(type).description(type.name()).build());
            }
        }
    }

    protected String registerAndLogin(String username, String password) {
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername(username);
        reg.setEmail(username + "@test.com");
        reg.setPassword(password);
        reg.setFullName("Test User");

        ResponseEntity<ApiResponse<LoginResponse>> regResp = restTemplate.exchange(
                "/api/v1/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(reg),
                new ParameterizedTypeReference<>() {});

        if (regResp.getStatusCode().is2xxSuccessful() && regResp.getBody() != null) {
            return regResp.getBody().getData().getToken();
        }

        // User might already exist — try login
        LoginRequest login = new LoginRequest();
        login.setUsername(username);
        login.setPassword(password);
        ResponseEntity<ApiResponse<LoginResponse>> loginResp = restTemplate.exchange(
                "/api/v1/auth/login",
                HttpMethod.POST,
                new HttpEntity<>(login),
                new ParameterizedTypeReference<>() {});
        return loginResp.getBody().getData().getToken();
    }

    protected HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
