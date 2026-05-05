package org.example.repository;

import org.example.entity.Context;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ContextRepository extends JpaRepository<Context, Long> {

    Optional<Context> findTopByProjectIdOrderByVersionDesc(Long projectId);

    List<Context> findByProjectIdOrderByVersionDesc(Long projectId);

    List<Context> findByProjectIdAndCreatedAtAfterOrderByVersionDesc(Long projectId, LocalDateTime after);

    @Query("SELECT COALESCE(MAX(c.version), 0) FROM Context c WHERE c.project.id = :projectId")
    Integer findMaxVersionByProjectId(@Param("projectId") Long projectId);

    Optional<Context> findByIdAndProjectId(Long id, Long projectId);
}
