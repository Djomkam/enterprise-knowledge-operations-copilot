package io.innovation.ekoc.teams.repository;

import io.innovation.ekoc.teams.domain.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    List<TeamMember> findByTeamIdAndActiveTrue(UUID teamId);

    List<TeamMember> findByUserIdAndActiveTrue(UUID userId);

    Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);

    boolean existsByTeamIdAndUserIdAndActiveTrue(UUID teamId, UUID userId);
}
