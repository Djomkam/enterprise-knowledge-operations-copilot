package io.innovation.ekoc.documents.domain;

import io.innovation.ekoc.shared.domain.BaseEntity;
import io.innovation.ekoc.teams.domain.Team;
import io.innovation.ekoc.users.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "documents", indexes = {
        @Index(name = "idx_doc_status", columnList = "status"),
        @Index(name = "idx_doc_team", columnList = "team_id"),
        @Index(name = "idx_doc_owner", columnList = "owner_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document extends BaseEntity {

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, length = 500)
    private String fileName;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private Long fileSizeBytes;

    @Column(nullable = false, length = 1000)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column
    private Integer chunkCount;

    @Column(length = 2000)
    private String errorMessage;

    /** UUID of the root document in this version chain (null = this is the root). */
    @Column(name = "parent_id")
    private UUID parentId;

    /** Business version counter (1-based); incremented on each re-upload. */
    @Column(nullable = false)
    @Builder.Default
    private Integer versionNumber = 1;
}
