package io.innovation.ekoc.chat.service;

import io.innovation.ekoc.chat.domain.Conversation;
import io.innovation.ekoc.chat.domain.Message;
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

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private static final int HISTORY_WINDOW = 10;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

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

    public String formatHistoryForPrompt(UUID conversationId) {
        List<Message> recent = messageRepository.findByConversationIdOrderByCreatedAtDesc(
                conversationId, PageRequest.of(0, HISTORY_WINDOW));
        if (recent.isEmpty()) {
            return null;
        }
        List<Message> ordered = recent.reversed();
        StringBuilder sb = new StringBuilder();
        for (Message m : ordered) {
            sb.append(m.getRole().name()).append(": ").append(m.getContent()).append("\n");
        }
        return sb.toString().trim();
    }

    @Transactional
    public Message saveMessage(Conversation conversation, io.innovation.ekoc.chat.domain.MessageRole role,
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
}
