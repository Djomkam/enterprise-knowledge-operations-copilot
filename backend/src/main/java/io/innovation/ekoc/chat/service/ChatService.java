package io.innovation.ekoc.chat.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.innovation.ekoc.ai.service.AIOrchestrationService;
import io.innovation.ekoc.chat.domain.Conversation;
import io.innovation.ekoc.chat.domain.MessageRole;
import io.innovation.ekoc.chat.dto.ChatRequest;
import io.innovation.ekoc.chat.dto.ChatResponse;
import io.innovation.ekoc.shared.exception.ResourceNotFoundException;
import io.innovation.ekoc.users.domain.User;
import io.innovation.ekoc.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final AIOrchestrationService orchestrationService;
    private final ConversationService conversationService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ChatResponse processMessage(ChatRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        // Get or create conversation
        Conversation conversation = conversationService.getOrCreate(
                request.getConversationId(), user, request.getMessage());

        // Save user message
        conversationService.saveMessage(conversation, MessageRole.USER, request.getMessage(), null, null);

        // Attach conversation history to request for context
        String history = conversationService.formatHistoryForPrompt(conversation.getId());
        ChatRequest enriched = ChatRequest.builder()
                .conversationId(conversation.getId())
                .message(request.getMessage())
                .includeContext(request.getIncludeContext())
                .build();

        // Run through agent pipeline
        ChatResponse aiResponse = orchestrationService.processChat(enriched, username);

        // Serialize citations for persistence
        String citationsJson = serializeCitations(aiResponse.getCitations());

        // Save assistant message
        var savedMessage = conversationService.saveMessage(
                conversation,
                MessageRole.ASSISTANT,
                aiResponse.getContent(),
                citationsJson,
                aiResponse.getTokensUsed());

        // Compress history if the conversation is long (async-safe, same transaction)
        conversationService.compressHistoryIfNeeded(conversation.getId());

        return ChatResponse.builder()
                .conversationId(conversation.getId())
                .messageId(savedMessage.getId())
                .content(aiResponse.getContent())
                .citations(aiResponse.getCitations())
                .tokensUsed(aiResponse.getTokensUsed())
                .build();
    }

    /**
     * Prepare a streaming session: create/get the conversation, save the user message.
     * Call {@link #finalizeStream} when the full assistant response is available.
     */
    @Transactional
    public StreamSession prepareStream(ChatRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        Conversation conversation = conversationService.getOrCreate(
                request.getConversationId(), user, request.getMessage());
        conversationService.saveMessage(conversation, MessageRole.USER, request.getMessage(), null, null);
        return new StreamSession(conversation.getId(), conversation);
    }

    /**
     * Persist the completed assistant response after the SSE stream closes.
     */
    @Transactional
    public void finalizeStream(StreamSession session, String content) {
        conversationService.saveMessage(session.conversation(), MessageRole.ASSISTANT, content, null, null);
        log.debug("Stream finalized for conversation={}, {} chars saved", session.conversationId(), content.length());
    }

    private String serializeCitations(List<ChatResponse.Citation> citations) {
        if (citations == null || citations.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(citations);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize citations: {}", e.getMessage());
            return null;
        }
    }

    public record StreamSession(java.util.UUID conversationId, Conversation conversation) {}
}
