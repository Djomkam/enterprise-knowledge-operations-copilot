package io.innovation.ekoc.ai.service.impl;

import io.innovation.ekoc.ai.dto.ChatCompletionRequest;
import io.innovation.ekoc.ai.dto.ChatCompletionResponse;
import io.innovation.ekoc.ai.service.ChatModelClient;
import io.innovation.ekoc.config.AIConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Service
public class ChatModelClientImpl implements ChatModelClient {

    private final ChatModel chatModel;

    public ChatModelClientImpl(
            AIConfig aiConfig,
            @Autowired(required = false) @Qualifier("openAiChatModel") ChatModel openAiChatModel,
            @Autowired(required = false) @Qualifier("ollamaChatModel") ChatModel ollamaChatModel) {

        String provider = aiConfig.getProvider();
        this.chatModel = switch (provider != null ? provider.toLowerCase() : "") {
            case "openai" -> {
                if (openAiChatModel == null) throw new IllegalStateException(
                        "ai.provider=openai but no OpenAI ChatModel bean found; check ai.openai.api-key");
                log.info("ChatModelClient using OpenAI");
                yield openAiChatModel;
            }
            case "ollama" -> {
                if (ollamaChatModel == null) throw new IllegalStateException(
                        "ai.provider=ollama but no Ollama ChatModel bean found; check ai.ollama.base-url");
                log.info("ChatModelClient using Ollama");
                yield ollamaChatModel;
            }
            default -> {
                log.warn("ai.provider='{}' — ChatModelClient is non-functional (test/none mode)", provider);
                yield null;
            }
        };
    }

    @Override
    public ChatCompletionResponse complete(ChatCompletionRequest request) {
        if (chatModel == null) {
            throw new IllegalStateException("No AI chat model is configured; set ai.provider to 'openai' or 'ollama'");
        }
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

    @Override
    public Flux<String> stream(ChatCompletionRequest request) {
        if (chatModel == null) {
            throw new IllegalStateException("No AI chat model is configured; set ai.provider to 'openai' or 'ollama'");
        }
        log.debug("Streaming {} messages to chat model", request.getMessages().size());
        List<Message> springMessages = request.getMessages().stream()
                .map(this::toSpringMessage)
                .toList();
        return chatModel.stream(new Prompt(springMessages))
                .map(response -> {
                    String content = response.getResult().getOutput().getContent();
                    return content != null ? content : "";
                });
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
