package org.example.repository;

import org.example.entity.Project;
import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByUserOrderByCreatedAtDesc(User user);
    long countByUser(User user);
    Optional<Project> findByIdAndUser(Long id, User user);
}
