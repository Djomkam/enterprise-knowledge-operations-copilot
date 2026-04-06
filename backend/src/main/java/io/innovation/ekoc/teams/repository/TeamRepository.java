package io.innovation.ekoc.teams.repository;

import io.innovation.ekoc.teams.domain.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    Page<Team> findByActiveTrue(Pageable pageable);

    Optional<Team> findByName(String name);

    boolean existsByName(String name);
}
