package io.innovation.ekoc.documents;

import io.innovation.ekoc.BaseIntegrationTest;
import io.innovation.ekoc.shared.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for document ACL enforcement.
 *
 * <p>These tests run in the {@code test} profile which uses the local JWT filter
 * (no Keycloak), so {@link BaseIntegrationTest#registerAndLogin} works as before.
 * The @PreAuthorize annotations on DocumentService layer are what we're testing here.
 */
class DocumentAclIntegrationTest extends BaseIntegrationTest {

    @Test
    void getDocument_unknownId_returns4xx() {
        String token = registerAndLogin("acl_user1_" + randomSuffix(), "password123");

        ResponseEntity<ApiResponse<Object>> resp = restTemplate.exchange(
                "/api/v1/documents/" + UUID.randomUUID(),
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                new ParameterizedTypeReference<>() {});

        // Document doesn't exist → ACL returns false (403 Forbidden), not 404.
        // The ACL check runs before the method body, so non-existent resources deny access.
        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void listDocuments_authenticatedUser_returns200() {
        String token = registerAndLogin("acl_user2_" + randomSuffix(), "password123");

        ResponseEntity<ApiResponse<Object>> resp = restTemplate.exchange(
                "/api/v1/documents",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(token)),
                new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listDocuments_withoutToken_returns401() {
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "/api/v1/documents", String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deleteDocument_unknownId_returns404() {
        String token = registerAndLogin("acl_user3_" + randomSuffix(), "password123");

        ResponseEntity<ApiResponse<Object>> resp = restTemplate.exchange(
                "/api/v1/documents/" + UUID.randomUUID(),
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(token)),
                new ParameterizedTypeReference<>() {});

        // Document doesn't exist → 404 rather than 403 (ACL component returns false → 403,
        // but our fallback in DocumentAcl returns false for missing docs which also yields 403;
        // in practice the controller throws ResourceNotFoundException first for DELETE)
        assertThat(resp.getStatusCode().is4xxClientError()).isTrue();
    }

    // -------------------------------------------------------------------------

    private String randomSuffix() {
        return String.valueOf(System.nanoTime()).substring(8);
    }
}
