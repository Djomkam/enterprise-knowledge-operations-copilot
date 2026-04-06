package io.innovation.ekoc.ai.service.impl;

import io.innovation.ekoc.ai.dto.ChatCompletionRequest;
import io.innovation.ekoc.ai.dto.ChatCompletionResponse;
import io.innovation.ekoc.ai.service.ChatModelClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatModelClientImpl implements ChatModelClient {

    private final ChatModel chatModel;

    @Override
    public ChatCompletionResponse complete(ChatCompletionRequest request) {
        log.debug("Sending {} messages to chat model", request.getMessages().size());

        List<Message> springMessages = request.getMessages().stream()
                .map(this::toSpringMessage)
                .toList();

        ChatResponse response = chatModel.call(new Prompt(springMessages));

        String content = response.getResult().getOutput().getContent();
        int tokensUsed = estimateTokens(content);

        log.debug("Chat model response: {} chars, ~{} tokens", content.length(), tokensUsed);

        return ChatCompletionResponse.builder()
                .content(content)
                .tokensUsed(tokensUsed)
                .finishReason(response.getResult().getMetadata().getFinishReason())
                .build();
    }

    private Message toSpringMessage(ChatCompletionRequest.ChatMessage msg) {
        return switch (msg.getRole().toLowerCase()) {
            case "system" -> new SystemMessage(msg.getContent());
            case "assistant" -> new AssistantMessage(msg.getContent());
            default -> new UserMessage(msg.getContent());
        };
    }

    private int estimateTokens(String text) {
        return Math.max(1, text.length() / 4);
    }
}
