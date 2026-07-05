package com.goldenmemories.repository;

import com.goldenmemories.model.ParentProfile;
import com.goldenmemories.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParentProfileRepository extends JpaRepository<ParentProfile, Long> {

    Optional<ParentProfile> findByOwner(User owner);

    boolean existsByOwner(User owner);
}
