package io.innovation.ekoc.teams.domain;

import io.innovation.ekoc.shared.domain.BaseEntity;
import io.innovation.ekoc.users.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "team_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "user_id"}),
    indexes = {
        @Index(name = "idx_team_member_team", columnList = "team_id"),
        @Index(name = "idx_team_member_user", columnList = "user_id")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String role; // OWNER, MEMBER, VIEWER

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
