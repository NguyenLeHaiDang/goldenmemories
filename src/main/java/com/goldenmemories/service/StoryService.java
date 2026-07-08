package com.goldenmemories.service;

import com.goldenmemories.model.Project;
import com.goldenmemories.model.StoryEntry;
import com.goldenmemories.model.StoryEntry.LifeStage;
import com.goldenmemories.model.StoryEntry.Status;

import java.util.List;
import java.util.Optional;

public interface StoryService {

    /** Add a new prompt to a project (status = PENDING). */
    StoryEntry addPrompt(Project project, String question, LifeStage lifeStage);

    /** Record raw transcript received from the parent (status -> TRANSCRIBED). */
    StoryEntry submitTranscript(Long storyId, String rawTranscript);

    /** Editor saves polished content and notes (status -> EDITED). */
    StoryEntry saveEdit(Long storyId, String editedContent, String editorNotes);

    /** Approve a story for inclusion in the memoir (status -> APPROVED). */
    StoryEntry approveStory(Long storyId);

    /** Revert an approved/edited story back to TRANSCRIBED for re-editing. */
    StoryEntry requestRevision(Long storyId);

    /** All stories for a project, ordered by creation date. */
    List<StoryEntry> listByProject(Project project);

    /** Stories in a specific status, for the editor queue. */
    List<StoryEntry> listByStatus(Status status);

    /** Find a single story by id. */
    Optional<StoryEntry> findById(Long id);

    /** Update QR-protection flag and access hint for a story entry (Phase 4). */
    StoryEntry updateQrConfig(Long storyId, boolean qrProtected, String qrHint);
}
