package io.innovation.ekoc.auth;

import io.innovation.ekoc.BaseIntegrationTest;
import io.innovation.ekoc.auth.dto.LoginRequest;
import io.innovation.ekoc.auth.dto.LoginResponse;
import io.innovation.ekoc.auth.dto.RegisterRequest;
import io.innovation.ekoc.shared.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends BaseIntegrationTest {

    @Test
    void register_success() {
        RegisterRequest req = RegisterRequest.builder()
                .username("newuser_auth_test")
                .email("newuser_auth_test@test.com")
                .password("password123")
                .fullName("New User")
                .build();

        ResponseEntity<ApiResponse<LoginResponse>> resp = restTemplate.exchange(
                "/api/v1/auth/register",
                HttpMethod.POST,
                new HttpEntity<>(req),
                new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getData().getToken()).isNotBlank();
        assertThat(resp.getBody().getData().getUsername()).isEqualTo("newuser_auth_test");
    }

    @Test
    void register_duplicateUsername_fails() {
        RegisterRequest req = RegisterRequest.builder()
                .username("dup_user_test")
                .email("dup_user_test@test.com")
                .password("password123")
                .build();

        restTemplate.exchange("/api/v1/auth/register", HttpMethod.POST,
                new HttpEntity<>(req), new ParameterizedTypeReference<ApiResponse<LoginResponse>>() {});

        // Same username, different email
        RegisterRequest dup = RegisterRequest.builder()
                .username("dup_user_test")
                .email("different@test.com")
                .password("password123")
                .build();

        ResponseEntity<ApiResponse<LoginResponse>> resp = restTemplate.exchange(
                "/api/v1/auth/register", HttpMethod.POST,
                new HttpEntity<>(dup), new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void login_success() {
        String token = registerAndLogin("login_test_user", "password123");
        assertThat(token).isNotBlank();
    }

    @Test
    void login_invalidCredentials_fails() {
        LoginRequest req = new LoginRequest();
        req.setUsername("nonexistent_xyz");
        req.setPassword("wrongpassword");

        ResponseEntity<ApiResponse<LoginResponse>> resp = restTemplate.exchange(
                "/api/v1/auth/login", HttpMethod.POST,
                new HttpEntity<>(req), new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() {
        ResponseEntity<String> resp = restTemplate.getForEntity("/api/v1/documents", String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
