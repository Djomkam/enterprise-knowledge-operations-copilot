package io.innovation.ekoc.ai.agent.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.innovation.ekoc.ai.agent.PlannerAgent;
import io.innovation.ekoc.ai.dto.ChatCompletionRequest;
import io.innovation.ekoc.ai.service.ChatModelClient;
import io.innovation.ekoc.retrieval.dto.SearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlannerAgentImpl implements PlannerAgent {

    private static final String SYSTEM_PROMPT = """
            You are a query planning assistant. Analyze the user query and decide whether
            document retrieval is needed to answer it.

            Respond ONLY with a JSON object in this exact format:
            {
              "needsRetrieval": true,
              "searchQuery": "<optimized semantic search query>",
              "reasoning": "<one-sentence explanation>"
            }

            Set needsRetrieval to false for greetings, simple math, or general knowledge
            questions that do not require internal documents.
            Set needsRetrieval to true for questions about company policies, internal docs,
            uploaded knowledge, or anything specific to an organization's context.
            """;

    private final ChatModelClient chatModelClient;
    private final ObjectMapper objectMapper;

    @Override
    public RetrievalPlan plan(String userQuery, String userId) {
        log.debug("Planning retrieval strategy for query: {}", userQuery);

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .messages(List.of(
                        ChatCompletionRequest.ChatMessage.builder()
                                .role("system").content(SYSTEM_PROMPT).build(),
                        ChatCompletionRequest.ChatMessage.builder()
                                .role("user").content(userQuery).build()
                ))
                .build();

        try {
            String raw = chatModelClient.complete(request).getContent();
            String json = extractJson(raw);
            JsonNode node = objectMapper.readTree(json);

            boolean needsRetrieval = node.path("needsRetrieval").asBoolean(true);
            String searchQuery = node.path("searchQuery").asText(userQuery);
            String reasoning = node.path("reasoning").asText("");

            SearchRequest searchRequest = SearchRequest.builder()
                    .query(searchQuery)
                    .build();

            log.debug("Plan: needsRetrieval={}, reasoning={}", needsRetrieval, reasoning);
            return new RetrievalPlan(needsRetrieval, searchRequest, reasoning);

        } catch (Exception e) {
            log.warn("PlannerAgent failed to parse model response, defaulting to retrieve: {}", e.getMessage());
            return new RetrievalPlan(true, SearchRequest.builder().query(userQuery).build(), "parse error — defaulting to retrieval");
        }
    }

    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }
}
