package io.innovation.ekoc.chat.controller;

import io.innovation.ekoc.ai.service.AIOrchestrationService;
import io.innovation.ekoc.chat.dto.ChatRequest;
import io.innovation.ekoc.chat.dto.ChatResponse;
import io.innovation.ekoc.chat.dto.ConversationDTO;
import io.innovation.ekoc.chat.dto.MessageDTO;
import io.innovation.ekoc.chat.service.ChatService;
import io.innovation.ekoc.chat.service.ConversationService;
import io.innovation.ekoc.shared.dto.ApiResponse;
import io.innovation.ekoc.shared.dto.PagedResponse;
import io.innovation.ekoc.shared.exception.UnauthorizedException;
import io.innovation.ekoc.shared.util.SecurityUtil;
import io.innovation.ekoc.users.domain.User;
import io.innovation.ekoc.users.repository.UserRepository;
import io.innovation.ekoc.shared.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Conversational AI endpoints")
@SecurityRequirement(name = "bearer-jwt")
public class ChatController {

    private final ChatService chatService;
    private final ConversationService conversationService;
    private final UserRepository userRepository;
    private final AIOrchestrationService orchestrationService;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream an AI-generated response token by token (SSE)")
    public SseEmitter streamChat(@Valid @RequestBody ChatRequest request) {
        String username = SecurityUtil.getCurrentUsername()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        log.info("Stream chat request from user={} conversationId={}", username, request.getConversationId());

        // Persist user message and create/get conversation before streaming starts
        ChatService.StreamSession session = chatService.prepareStream(request, username);

        ChatRequest enriched = ChatRequest.builder()
                .conversationId(session.conversationId())
                .message(request.getMessage())
                .includeContext(request.getIncludeContext())
                .build();

        Flux<String> tokenStream = orchestrationService.streamChat(enriched, username);
        SseEmitter emitter = new SseEmitter(90_000L);
        StringBuilder fullContent = new StringBuilder();

        tokenStream.subscribe(
                token -> {
                    if (!token.isEmpty()) {
                        fullContent.append(token);
                        try {
                            emitter.send(SseEmitter.event().name("token").data(token));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    }
                },
                error -> {
                    log.error("Stream error for user={}", username, error);
                    emitter.completeWithError(error);
                },
                () -> {
                    try {
                        chatService.finalizeStream(session, fullContent.toString());
                        emitter.send(SseEmitter.event().name("done").data(session.conversationId().toString()));
                        emitter.complete();
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                }
        );

        return emitter;
    }

    @PostMapping
    @Operation(summary = "Send a message and receive an AI-generated response")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@Valid @RequestBody ChatRequest request) {
        String username = SecurityUtil.getCurrentUsername()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        log.info("Chat request from user={} conversationId={}", username, request.getConversationId());
        ChatResponse response = chatService.processMessage(request, username);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/conversations")
    @Operation(summary = "List conversations for the current user")
    public ResponseEntity<ApiResponse<PagedResponse<ConversationDTO>>> listConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String username = SecurityUtil.getCurrentUsername()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));

        Page<ConversationDTO> conversations = conversationService.listForUsername(
                username, PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(ConversationDTO::from);

        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(conversations)));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Retrieve message history for a conversation")
    public ResponseEntity<ApiResponse<List<MessageDTO>>> getMessages(
            @PathVariable UUID conversationId) {
        String username = SecurityUtil.getCurrentUsername()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        List<MessageDTO> messages = conversationService.getHistoryForUser(conversationId, user.getId())
                .stream().map(MessageDTO::from).toList();
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @DeleteMapping("/conversations/{conversationId}")
    @Operation(summary = "Delete (deactivate) a conversation")
    public ResponseEntity<Void> deleteConversation(@PathVariable UUID conversationId) {
        String username = SecurityUtil.getCurrentUsername()
                .orElseThrow(() -> new UnauthorizedException("Not authenticated"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        conversationService.deactivate(conversationId, user.getId());
        log.info("User {} deleted conversation {}", username, conversationId);
        return ResponseEntity.noContent().build();
    }
}
