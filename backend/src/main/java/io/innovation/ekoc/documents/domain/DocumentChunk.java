package io.innovation.ekoc.documents.domain;

import io.innovation.ekoc.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import com.pgvector.PGvector;

@Entity
@Table(name = "document_chunks", indexes = {
        @Index(name = "idx_chunk_document", columnList = "document_id"),
        @Index(name = "idx_chunk_position", columnList = "document_id, position")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Integer tokenCount;

    @Column(columnDefinition = "vector(1536)")
    private PGvector embedding;

    @Column(length = 2000)
    private String metadata;
}
