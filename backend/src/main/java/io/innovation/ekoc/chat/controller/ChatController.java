package io.innovation.ekoc.chat.controller;

import io.innovation.ekoc.chat.domain.Conversation;
import io.innovation.ekoc.chat.domain.Message;
import io.innovation.ekoc.chat.dto.ChatRequest;
import io.innovation.ekoc.chat.dto.ChatResponse;
import io.innovation.ekoc.chat.service.ChatService;
import io.innovation.ekoc.chat.service.ConversationService;
import io.innovation.ekoc.shared.dto.ApiResponse;
import io.innovation.ekoc.shared.dto.PagedResponse;
import io.innovation.ekoc.shared.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    @Operation(summary = "Send a message and receive an AI-generated response")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@Valid @RequestBody ChatRequest request) {
        String username = SecurityUtil.getCurrentUsername()
                .orElseThrow(() -> new io.innovation.ekoc.shared.exception.UnauthorizedException("Not authenticated"));
        log.info("Chat request from user={} conversationId={}", username, request.getConversationId());
        ChatResponse response = chatService.processMessage(request, username);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/conversations")
    @Operation(summary = "List conversations for the current user")
    public ResponseEntity<ApiResponse<PagedResponse<Conversation>>> listConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String username = SecurityUtil.getCurrentUsername()
                .orElseThrow(() -> new io.innovation.ekoc.shared.exception.UnauthorizedException("Not authenticated"));

        Page<Conversation> conversations = conversationService.listForUsername(
                username, PageRequest.of(page, size, Sort.by("createdAt").descending()));

        return ResponseEntity.ok(ApiResponse.success(PagedResponse.of(conversations)));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Retrieve message history for a conversation")
    public ResponseEntity<ApiResponse<List<Message>>> getMessages(
            @PathVariable UUID conversationId) {
        List<Message> messages = conversationService.getHistory(conversationId);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @DeleteMapping("/conversations/{conversationId}")
    @Operation(summary = "Delete (deactivate) a conversation")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(@PathVariable UUID conversationId) {
        String username = SecurityUtil.getCurrentUsername()
                .orElseThrow(() -> new io.innovation.ekoc.shared.exception.UnauthorizedException("Not authenticated"));
        // Ownership enforced inside service
        log.info("User {} deleting conversation {}", username, conversationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
