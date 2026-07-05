package com.goldenmemories.model;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The central aggregate for a memoir project.
 * One user can have one active project at a time.
 */
@Entity
@Table(name = "project")
public class Project {

    public enum Phase {
        ONBOARDING,
        STORY_COLLECTION,
        EDITING,
        DRAFT_REVIEW,
        PUBLISHING,
        COMPLETED
    }

    public enum Package {
        BASIC,
        STANDARD,
        PREMIUM
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Package selectedPackage = Package.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Phase currentPhase = Phase.ONBOARDING;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<StoryEntry> stories = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PhotoAsset> photos = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ApprovalRecord> approvals = new ArrayList<>();

    // ── Convenience ──────────────────────────────────────────────────────────

    /** Advance to the next phase. No-op if already COMPLETED. */
    public void advancePhase() {
        Phase[] phases = Phase.values();
        int next = currentPhase.ordinal() + 1;
        if (next < phases.length) {
            this.currentPhase = phases[next];
            this.updatedAt = Instant.now();
        }
    }

    public int storyCount()  { return stories.size(); }
    public int photoCount()  { return photos.size(); }

    // ── Getters & setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Package getSelectedPackage() { return selectedPackage; }
    public void setSelectedPackage(Package selectedPackage) { this.selectedPackage = selectedPackage; }

    public Phase getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(Phase currentPhase) { this.currentPhase = currentPhase; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<StoryEntry> getStories() { return stories; }
    public List<PhotoAsset> getPhotos()  { return photos; }
    public List<ApprovalRecord> getApprovals() { return approvals; }
}
