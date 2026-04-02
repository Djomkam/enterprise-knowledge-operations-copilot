package io.innovation.ekoc.ai.agent.impl;

import io.innovation.ekoc.ai.agent.AnswerComposer;
import io.innovation.ekoc.ai.dto.ChatCompletionRequest;
import io.innovation.ekoc.ai.service.ChatModelClient;
import io.innovation.ekoc.chat.dto.ChatResponse;
import io.innovation.ekoc.retrieval.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnswerComposerImpl implements AnswerComposer {

    private static final String SYSTEM_PROMPT = """
            You are a helpful enterprise knowledge assistant. Answer the user's question
            using ONLY the provided context documents. Follow these rules:

            1. Cite sources using [Doc: <document title>] inline when you use information from them.
            2. If the context does not contain enough information to answer, say:
               "I don't have enough information in the available documents to answer that."
            3. Do not hallucinate or invent facts not present in the context.
            4. Be concise and direct.
            """;

    private final ChatModelClient chatModelClient;

    @Override
    public ChatResponse compose(String query, List<SearchResult> context, String conversationHistory) {
        log.debug("Composing answer from {} context chunks", context.size());

        String contextBlock = buildContextBlock(context);
        String userContent = buildUserContent(query, contextBlock, conversationHistory);

        List<ChatCompletionRequest.ChatMessage> messages = new ArrayList<>();
        messages.add(ChatCompletionRequest.ChatMessage.builder()
                .role("system").content(SYSTEM_PROMPT).build());
        messages.add(ChatCompletionRequest.ChatMessage.builder()
                .role("user").content(userContent).build());

        var completionRequest = ChatCompletionRequest.builder().messages(messages).build();
        var completion = chatModelClient.complete(completionRequest);

        List<ChatResponse.Citation> citations = extractCitations(completion.getContent(), context);

        return ChatResponse.builder()
                .content(completion.getContent())
                .citations(citations)
                .tokensUsed(completion.getTokensUsed())
                .build();
    }

    private String buildContextBlock(List<SearchResult> context) {
        if (context.isEmpty()) {
            return "(No relevant documents found.)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < context.size(); i++) {
            SearchResult r = context.get(i);
            sb.append("[").append(i + 1).append("] ")
              .append(r.getDocumentTitle()).append(":\n")
              .append(r.getContent()).append("\n\n");
        }
        return sb.toString().trim();
    }

    private String buildUserContent(String query, String contextBlock, String conversationHistory) {
        StringBuilder sb = new StringBuilder();
        sb.append("Context documents:\n").append(contextBlock);
        if (conversationHistory != null && !conversationHistory.isBlank()) {
            sb.append("\n\nConversation history:\n").append(conversationHistory);
        }
        sb.append("\n\nQuestion: ").append(query);
        return sb.toString();
    }

    private List<ChatResponse.Citation> extractCitations(String responseContent, List<SearchResult> context) {
        List<ChatResponse.Citation> citations = new ArrayList<>();
        for (SearchResult result : context) {
            if (responseContent.contains(result.getDocumentTitle())) {
                citations.add(ChatResponse.Citation.builder()
                        .documentId(result.getDocumentId())
                        .documentTitle(result.getDocumentTitle())
                        .snippet(truncate(result.getContent(), 200))
                        .relevanceScore(result.getSimilarityScore())
                        .build());
            }
        }
        return citations;
    }

    private String truncate(String text, int maxLen) {
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
