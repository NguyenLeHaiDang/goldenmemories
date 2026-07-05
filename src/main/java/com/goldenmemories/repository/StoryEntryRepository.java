package com.goldenmemories.repository;

import com.goldenmemories.model.Project;
import com.goldenmemories.model.StoryEntry;
import com.goldenmemories.model.StoryEntry.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoryEntryRepository extends JpaRepository<StoryEntry, Long> {

    List<StoryEntry> findByProjectOrderByCreatedAtAsc(Project project);

    long countByProjectAndStatus(Project project, Status status);

    long countByProject(Project project);
}
