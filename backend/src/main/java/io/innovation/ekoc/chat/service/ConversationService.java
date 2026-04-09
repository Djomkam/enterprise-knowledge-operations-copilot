package io.innovation.ekoc.chat.service;

import io.innovation.ekoc.ai.dto.ChatCompletionRequest;
import io.innovation.ekoc.ai.service.ChatModelClient;
import io.innovation.ekoc.audit.annotation.Auditable;
import io.innovation.ekoc.audit.domain.AuditAction;
import io.innovation.ekoc.chat.domain.Conversation;
import io.innovation.ekoc.chat.domain.Message;
import io.innovation.ekoc.chat.domain.MessageRole;
import io.innovation.ekoc.chat.repository.ConversationRepository;
import io.innovation.ekoc.chat.repository.MessageRepository;
import io.innovation.ekoc.shared.exception.ResourceNotFoundException;
import io.innovation.ekoc.users.domain.User;
import io.innovation.ekoc.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    /** After this many messages, compress older messages into a summary. */
    private static final int COMPRESSION_THRESHOLD = 20;
    /** Keep this many recent messages verbatim after compression. */
    private static final int RECENT_WINDOW = 6;
    /** When building prompt history without summary, include this many messages. */
    private static final int HISTORY_WINDOW = 10;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ChatModelClient chatModelClient;

    @Transactional
    public Conversation getOrCreate(UUID conversationId, User user, String firstMessagePreview) {
        if (conversationId != null) {
            return conversationRepository.findByIdAndUserId(conversationId, user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        }
        String title = firstMessagePreview.length() > 80
                ? firstMessagePreview.substring(0, 77) + "..."
                : firstMessagePreview;
        Conversation conversation = Conversation.builder()
                .title(title)
                .user(user)
                .build();
        return conversationRepository.save(conversation);
    }

    public Page<Conversation> listForUser(UUID userId, Pageable pageable) {
        return conversationRepository.findByUserIdAndActiveTrue(userId, pageable);
    }

    public Page<Conversation> listForUsername(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return conversationRepository.findByUserIdAndActiveTrue(user.getId(), pageable);
    }

    @Auditable(action = AuditAction.CHAT_DELETE, resource = "Conversation")
    @Transactional
    public void deactivate(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        conversation.setActive(false);
        conversationRepository.save(conversation);
    }

    public List<Message> getHistory(UUID conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    public List<Message> getHistoryForUser(UUID conversationId, UUID userId) {
        conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    /**
     * Returns a formatted history string for the LLM prompt.
     * If a SUMMARY message exists, uses: summary + last {@code RECENT_WINDOW} non-summary messages.
     * Otherwise falls back to the last {@code HISTORY_WINDOW} messages.
     */
    public String formatHistoryForPrompt(UUID conversationId) {
        // Check for an existing summary
        var summaryMsg = messageRepository
                .findTopByConversationIdAndRoleOrderByCreatedAtDesc(conversationId, MessageRole.SUMMARY);

        if (summaryMsg.isPresent()) {
            Message summary = summaryMsg.get();
            // Get recent messages since the summary
            List<Message> recent = messageRepository.findByConversationIdOrderByCreatedAtDesc(
                    conversationId, PageRequest.of(0, RECENT_WINDOW));
            List<Message> ordered = recent.reversed().stream()
                    .filter(m -> m.getRole() != MessageRole.SUMMARY)
                    .toList();

            StringBuilder sb = new StringBuilder("[Earlier summary]: ")
                    .append(summary.getContent())
                    .append("\n\n[Recent messages]:\n");
            for (Message m : ordered) {
                sb.append(m.getRole().name()).append(": ").append(m.getContent()).append("\n");
            }
            return sb.toString().trim();
        }

        List<Message> recent = messageRepository.findByConversationIdOrderByCreatedAtDesc(
                conversationId, PageRequest.of(0, HISTORY_WINDOW));
        if (recent.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        for (Message m : recent.reversed()) {
            sb.append(m.getRole().name()).append(": ").append(m.getContent()).append("\n");
        }
        return sb.toString().trim();
    }

    @Transactional
    public Message saveMessage(Conversation conversation, MessageRole role,
                               String content, String citations, Integer tokensUsed) {
        Message message = Message.builder()
                .conversation(conversation)
                .role(role)
                .content(content)
                .citations(citations)
                .tokensUsed(tokensUsed)
                .build();
        Message saved = messageRepository.save(message);

        conversation.setMessageCount(
                (conversation.getMessageCount() == null ? 0 : conversation.getMessageCount()) + 1);
        conversationRepository.save(conversation);

        return saved;
    }

    /**
     * If the conversation has exceeded {@code COMPRESSION_THRESHOLD} messages, compress
     * older messages into a SUMMARY message and delete the originals.
     * Safe to call after every assistant turn; no-ops when below threshold.
     */
    @Transactional
    public void compressHistoryIfNeeded(UUID conversationId) {
        long count = messageRepository.countByConversationId(conversationId);
        if (count <= COMPRESSION_THRESHOLD) return;

        // Don't compress if a recent summary already covers most of the history
        var existingSummary = messageRepository
                .findTopByConversationIdAndRoleOrderByCreatedAtDesc(conversationId, MessageRole.SUMMARY);

        // Collect the messages to compress: all except the last RECENT_WINDOW non-summary messages
        List<Message> allMessages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        List<Message> nonSummary = allMessages.stream()
                .filter(m -> m.getRole() != MessageRole.SUMMARY)
                .toList();

        if (nonSummary.size() <= RECENT_WINDOW) return; // Nothing old enough to compress

        int cutIndex = nonSummary.size() - RECENT_WINDOW;
        List<Message> toCompress = nonSummary.subList(0, cutIndex);
        Instant cutoffTime = toCompress.get(toCompress.size() - 1).getCreatedAt();

        String historyText = toCompress.stream()
                .map(m -> m.getRole().name() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        // If there's an existing summary, include it in the new compression
        if (existingSummary.isPresent()) {
            historyText = "[Previous summary]: " + existingSummary.get().getContent()
                    + "\n\n[New messages to summarize]:\n" + historyText;
        }

        String summaryText = callSummarizer(historyText);
        if (summaryText == null || summaryText.isBlank()) {
            log.warn("Summarizer returned empty result for conversationId={}, skipping compression", conversationId);
            return;
        }

        // Persist the new summary
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        Message summary = Message.builder()
                .conversation(conversation)
                .role(MessageRole.SUMMARY)
                .content(summaryText)
                .build();
        messageRepository.save(summary);

        // Delete the old summary (if any) and the compressed messages
        existingSummary.ifPresent(messageRepository::delete);
        messageRepository.deleteOlderThan(conversationId, MessageRole.SUMMARY, cutoffTime.plusNanos(1));

        log.info("Compressed {} messages into summary for conversationId={}", toCompress.size(), conversationId);
    }

    private String callSummarizer(String historyText) {
        try {
            var request = ChatCompletionRequest.builder()
                    .messages(List.of(
                            ChatCompletionRequest.ChatMessage.builder()
                                    .role("system")
                                    .content("Summarize the following conversation history concisely, " +
                                             "preserving key facts, decisions, and context. " +
                                             "Write in third person. Be factual and brief.")
                                    .build(),
                            ChatCompletionRequest.ChatMessage.builder()
                                    .role("user")
                                    .content(historyText)
                                    .build()))
                    .build();
            return chatModelClient.complete(request).getContent();
        } catch (Exception e) {
            log.error("Failed to summarize conversation history: {}", e.getMessage());
            return null;
        }
    }
}
