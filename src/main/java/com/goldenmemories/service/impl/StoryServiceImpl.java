package com.goldenmemories.service.impl;

import com.goldenmemories.model.Project;
import com.goldenmemories.model.StoryEntry;
import com.goldenmemories.model.StoryEntry.LifeStage;
import com.goldenmemories.model.StoryEntry.Status;
import com.goldenmemories.repository.StoryEntryRepository;
import com.goldenmemories.service.StoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class StoryServiceImpl implements StoryService {

    private final StoryEntryRepository storyEntryRepository;

    public StoryServiceImpl(StoryEntryRepository storyEntryRepository) {
        this.storyEntryRepository = storyEntryRepository;
    }

    @Override
    public StoryEntry addPrompt(Project project, String question, LifeStage lifeStage) {
        StoryEntry entry = new StoryEntry();
        entry.setProject(project);
        entry.setPromptQuestion(question);
        entry.setLifeStage(lifeStage);
        entry.setStatus(Status.PENDING);
        return storyEntryRepository.save(entry);
    }

    @Override
    public StoryEntry submitTranscript(Long storyId, String rawTranscript) {
        StoryEntry entry = getOrThrow(storyId);
        entry.setRawTranscript(rawTranscript);
        entry.setStatus(Status.TRANSCRIBED);
        return storyEntryRepository.save(entry);
    }

    @Override
    public StoryEntry saveEdit(Long storyId, String editedContent, String editorNotes) {
        StoryEntry entry = getOrThrow(storyId);
        entry.setEditedContent(editedContent);
        entry.setEditorNotes(editorNotes);
        entry.setStatus(Status.EDITED);
        return storyEntryRepository.save(entry);
    }

    @Override
    public StoryEntry approveStory(Long storyId) {
        StoryEntry entry = getOrThrow(storyId);
        entry.setStatus(Status.APPROVED);
        return storyEntryRepository.save(entry);
    }

    @Override
    public StoryEntry requestRevision(Long storyId) {
        StoryEntry entry = getOrThrow(storyId);
        entry.setStatus(Status.TRANSCRIBED);
        return storyEntryRepository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryEntry> listByProject(Project project) {
        return storyEntryRepository.findByProjectOrderByCreatedAtAsc(project);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryEntry> listByStatus(Status status) {
        return storyEntryRepository.findByStatusOrderByUpdatedAtAsc(status);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoryEntry> findById(Long id) {
        return storyEntryRepository.findById(id);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private StoryEntry getOrThrow(Long id) {
        return storyEntryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Story not found: " + id));
    }
}
