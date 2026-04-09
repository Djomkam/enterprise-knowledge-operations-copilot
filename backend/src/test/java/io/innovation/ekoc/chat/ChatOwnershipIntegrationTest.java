package io.innovation.ekoc.chat;

import io.innovation.ekoc.BaseIntegrationTest;
import io.innovation.ekoc.ai.service.AIOrchestrationService;
import io.innovation.ekoc.chat.dto.ChatRequest;
import io.innovation.ekoc.chat.dto.ChatResponse;
import io.innovation.ekoc.chat.dto.ConversationDTO;
import io.innovation.ekoc.shared.dto.ApiResponse;
import io.innovation.ekoc.shared.dto.PagedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ChatOwnershipIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private AIOrchestrationService aiOrchestrationService;

    private String userAToken;
    private String userBToken;

    @BeforeEach
    void setUp() {
        ChatResponse mockResponse = ChatResponse.builder()
                .conversationId(UUID.randomUUID())
                .messageId(UUID.randomUUID())
                .content("Test response")
                .build();
        when(aiOrchestrationService.processChat(any(), any())).thenReturn(mockResponse);

        userAToken = registerAndLogin("chat_owner_a_" + System.nanoTime(), "password123");
        userBToken = registerAndLogin("chat_owner_b_" + System.nanoTime(), "password123");
    }

    @Test
    void getMessages_ownConversation_succeeds() {
        UUID conversationId = createConversation(userAToken);

        ResponseEntity<ApiResponse<List<?>>> resp = restTemplate.exchange(
                "/api/v1/chat/conversations/" + conversationId + "/messages",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userAToken)),
                new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getMessages_otherUsersConversation_returns404() {
        UUID conversationId = createConversation(userAToken);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/api/v1/chat/conversations/" + conversationId + "/messages",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userBToken)),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteConversation_ownConversation_succeeds() {
        UUID conversationId = createConversation(userAToken);

        ResponseEntity<Void> resp = restTemplate.exchange(
                "/api/v1/chat/conversations/" + conversationId,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(userAToken)),
                Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void deleteConversation_otherUsersConversation_returns404() {
        UUID conversationId = createConversation(userAToken);

        ResponseEntity<String> resp = restTemplate.exchange(
                "/api/v1/chat/conversations/" + conversationId,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(userBToken)),
                String.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deletedConversation_notInList() {
        UUID conversationId = createConversation(userAToken);

        restTemplate.exchange(
                "/api/v1/chat/conversations/" + conversationId,
                HttpMethod.DELETE,
                new HttpEntity<>(bearerHeaders(userAToken)),
                Void.class);

        ResponseEntity<ApiResponse<PagedResponse<ConversationDTO>>> listResp = restTemplate.exchange(
                "/api/v1/chat/conversations",
                HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(userAToken)),
                new ParameterizedTypeReference<>() {});

        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<ConversationDTO> conversations = listResp.getBody().getData().getContent();
        assertThat(conversations).noneMatch(c -> c.getId().equals(conversationId));
    }

    private UUID createConversation(String token) {
        ChatRequest req = new ChatRequest();
        req.setMessage("Hello, test message");

        ChatResponse fakeResponse = ChatResponse.builder()
                .conversationId(UUID.randomUUID())
                .messageId(UUID.randomUUID())
                .content("Mock response")
                .build();
        when(aiOrchestrationService.processChat(any(), any())).thenReturn(fakeResponse);

        ResponseEntity<ApiResponse<ChatResponse>> resp = restTemplate.exchange(
                "/api/v1/chat",
                HttpMethod.POST,
                new HttpEntity<>(req, bearerHeaders(token)),
                new ParameterizedTypeReference<>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resp.getBody().getData().getConversationId();
    }
}
