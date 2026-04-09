package io.innovation.ekoc.chat.domain;

public enum MessageRole {
    USER,
    ASSISTANT,
    SYSTEM,
    /** Compressed history summary stored when conversation exceeds the compression threshold. */
    SUMMARY
}
