package io.innovation.ekoc.chat.domain;

import io.innovation.ekoc.shared.domain.BaseEntity;
import io.innovation.ekoc.teams.domain.Team;
import io.innovation.ekoc.users.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_conv_user", columnList = "user_id"),
        @Index(name = "idx_conv_team", columnList = "team_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation extends BaseEntity {

    @Column(nullable = false, length = 500)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column
    private Integer messageCount;
}
