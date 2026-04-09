package io.innovation.ekoc.prompttemplate.domain;

import io.innovation.ekoc.shared.domain.BaseEntity;
import io.innovation.ekoc.teams.domain.Team;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "prompt_templates", indexes = {
        @Index(name = "idx_pt_role",   columnList = "role_type"),
        @Index(name = "idx_pt_team",   columnList = "team_id"),
        @Index(name = "idx_pt_active", columnList = "active")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTemplate extends BaseEntity {

    @Column(nullable = false, length = 200, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String systemPrompt;

    /** Optional: only applies to users with this role type (null = applies to all). */
    @Column(length = 50)
    private String roleType;

    /** Optional: scoped to a specific team (null = global). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
