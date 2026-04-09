package io.innovation.ekoc.analytics.repository;

import io.innovation.ekoc.analytics.domain.QueryAnalytic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface QueryAnalyticRepository extends JpaRepository<QueryAnalytic, UUID> {

    Page<QueryAnalytic> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<QueryAnalytic> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<QueryAnalytic> findByTeamIdOrderByCreatedAtDesc(UUID teamId, Pageable pageable);

    Page<QueryAnalytic> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to, Pageable pageable);

    @Query("SELECT AVG(q.latencyMs) FROM QueryAnalytic q WHERE q.createdAt BETWEEN :from AND :to")
    Double avgLatencyBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(q) FROM QueryAnalytic q WHERE q.createdAt BETWEEN :from AND :to AND q.success = true")
    long countSuccessBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT COUNT(q) FROM QueryAnalytic q WHERE q.createdAt BETWEEN :from AND :to")
    long countTotalBetween(@Param("from") Instant from, @Param("to") Instant to);
}
