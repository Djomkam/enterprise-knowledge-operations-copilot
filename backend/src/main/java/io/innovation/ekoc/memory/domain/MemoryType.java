package io.innovation.ekoc.memory.domain;

public enum MemoryType {
    SHORT_TERM,      // Recent conversation context
    LONG_TERM,       // Persistent user facts and preferences
    ENTITY,          // Extracted entities from conversations
    SEMANTIC         // Semantically important snippets
}
