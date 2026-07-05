package com.goldenmemories.repository;

import com.goldenmemories.model.Project;
import com.goldenmemories.model.StoryEntry;
import com.goldenmemories.model.StoryEntry.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoryEntryRepository extends JpaRepository<StoryEntry, Long> {

    List<StoryEntry> findByProjectOrderByCreatedAtAsc(Project project);

    /** Editor queue: all stories with a given status, oldest-first. */
    List<StoryEntry> findByStatusOrderByUpdatedAtAsc(Status status);

    /** Stories for a project filtered by status. */
    List<StoryEntry> findByProjectAndStatusOrderByCreatedAtAsc(Project project, Status status);

    long countByProjectAndStatus(Project project, Status status);

    long countByProject(Project project);
}
