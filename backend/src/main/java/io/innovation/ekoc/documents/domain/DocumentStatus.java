package io.innovation.ekoc.documents.domain;

public enum DocumentStatus {
    PENDING,
    PROCESSING,
    CHUNKING,
    EMBEDDING,
    COMPLETED,
    FAILED
}
