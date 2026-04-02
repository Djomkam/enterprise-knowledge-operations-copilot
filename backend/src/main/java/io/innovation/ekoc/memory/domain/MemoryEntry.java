package io.innovation.ekoc.memory.domain;

import com.pgvector.PGvector;
import io.innovation.ekoc.shared.domain.BaseEntity;
import io.innovation.ekoc.users.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "memory_entries", indexes = {
        @Index(name = "idx_memory_user", columnList = "user_id"),
        @Index(name = "idx_memory_type", columnList = "memory_type")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, name = "memory_type")
    private MemoryType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(columnDefinition = "vector(1536)")
    private PGvector embedding;

    @Column
    private Double relevanceScore;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
