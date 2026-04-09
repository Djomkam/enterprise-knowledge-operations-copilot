package io.innovation.ekoc.prompttemplate.repository;

import io.innovation.ekoc.prompttemplate.domain.PromptTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, UUID> {

    Page<PromptTemplate> findByActiveTrueOrderByNameAsc(Pageable pageable);

    /** Team-scoped template takes precedence over role-scoped, which takes precedence over default. */
    Optional<PromptTemplate> findTopByTeamIdAndActiveTrueOrderByUpdatedAtDesc(UUID teamId);

    Optional<PromptTemplate> findTopByRoleTypeAndTeamIdIsNullAndActiveTrueOrderByUpdatedAtDesc(String roleType);

    Optional<PromptTemplate> findTopByNameAndActiveTrue(String name);
}
