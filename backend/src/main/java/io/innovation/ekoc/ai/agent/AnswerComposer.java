package io.innovation.ekoc.ai.agent;

import io.innovation.ekoc.chat.dto.ChatResponse;
import io.innovation.ekoc.retrieval.dto.SearchResult;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AnswerComposer {

    /**
     * Compose a grounded answer (blocking).
     *
     * @param systemPromptOverride when non-null, replaces the default system prompt
     *                             (used to apply role-/team-scoped prompt templates).
     */
    ChatResponse compose(
            String query,
            List<SearchResult> context,
            String conversationHistory,
            String systemPromptOverride
    );

    /**
     * Stream answer tokens for SSE delivery.
     *
     * @param systemPromptOverride same as for {@link #compose}
     */
    Flux<String> streamCompose(
            String query,
            List<SearchResult> context,
            String conversationHistory,
            String systemPromptOverride
    );
}
