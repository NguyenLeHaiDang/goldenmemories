package com.goldenmemories.repository;

import com.goldenmemories.model.Project;
import com.goldenmemories.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByOwnerOrderByCreatedAtDesc(User owner);

    /** Load first project for the owner (most recently created). */
    Optional<Project> findFirstByOwnerOrderByCreatedAtDesc(User owner);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.owner = :owner")
    long countByOwner(User owner);
}
