package io.innovation.ekoc.analytics.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "query_analytics", indexes = {
        @Index(name = "idx_qa_user",    columnList = "user_id"),
        @Index(name = "idx_qa_team",    columnList = "team_id"),
        @Index(name = "idx_qa_created", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryAnalytic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "team_id")
    private UUID teamId;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String query;

    private Integer responseLength;

    @Column(name = "retrieval_hits")
    @Builder.Default
    private Integer retrievalHits = 0;

    private Long latencyMs;

    private Integer tokensUsed;

    @Column(length = 100)
    private String modelUsed;

    @Column(nullable = false)
    @Builder.Default
    private boolean success = true;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
