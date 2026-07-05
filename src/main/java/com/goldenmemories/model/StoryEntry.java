package com.goldenmemories.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A single memory or life-story entry contributed by the parent.
 */
@Entity
@Table(name = "story_entry")
public class StoryEntry {

    public enum LifeStage {
        CHILDHOOD,
        YOUTH,
        ADULTHOOD,
        FAMILY_LIFE,
        CAREER,
        LATER_LIFE,
        OTHER
    }

    public enum Status {
        /** Prompt sent, awaiting parent response. */
        PENDING,
        /** Voice/text received, not yet transcribed. */
        RECEIVED,
        /** Transcript generated, awaiting editor review. */
        TRANSCRIBED,
        /** Editor has reviewed and polished the text. */
        EDITED,
        /** Approved for inclusion in the memoir. */
        APPROVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String promptQuestion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LifeStage lifeStage = LifeStage.OTHER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    /** Raw transcript from voice note or direct text input. */
    @Column(length = 10000)
    private String rawTranscript;

    /** Editor-polished version of the transcript. */
    @Column(length = 10000)
    private String editedContent;

    /** Internal notes from the editor. */
    @Column(length = 2000)
    private String editorNotes;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    // ── Getters & setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getPromptQuestion() { return promptQuestion; }
    public void setPromptQuestion(String promptQuestion) { this.promptQuestion = promptQuestion; }

    public LifeStage getLifeStage() { return lifeStage; }
    public void setLifeStage(LifeStage lifeStage) { this.lifeStage = lifeStage; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public String getRawTranscript() { return rawTranscript; }
    public void setRawTranscript(String rawTranscript) { this.rawTranscript = rawTranscript; }

    public String getEditedContent() { return editedContent; }
    public void setEditedContent(String editedContent) { this.editedContent = editedContent; }

    public String getEditorNotes() { return editorNotes; }
    public void setEditorNotes(String editorNotes) { this.editorNotes = editorNotes; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
