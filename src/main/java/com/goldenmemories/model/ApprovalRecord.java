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
 * Records a reviewer's decision on a memoir draft version.
 */
@Entity
@Table(name = "approval_record")
public class ApprovalRecord {

    public enum Decision {
        PENDING,
        APPROVED,
        REJECTED,
        REVISION_REQUESTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /** e.g. "v1", "v2" */
    @Column(nullable = false)
    private String draftVersion;

    /** The user who submitted the review decision. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Decision decision = Decision.PENDING;

    @Column(length = 3000)
    private String comments;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant decidedAt;

    // ── Getters & setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getDraftVersion() { return draftVersion; }
    public void setDraftVersion(String draftVersion) { this.draftVersion = draftVersion; }

    public User getReviewer() { return reviewer; }
    public void setReviewer(User reviewer) { this.reviewer = reviewer; }

    public Decision getDecision() { return decision; }
    public void setDecision(Decision decision) {
        this.decision = decision;
        if (decision != Decision.PENDING) {
            this.decidedAt = Instant.now();
        }
    }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getDecidedAt() { return decidedAt; }
}
