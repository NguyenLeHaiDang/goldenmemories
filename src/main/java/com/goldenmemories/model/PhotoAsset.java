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
 * A family photo uploaded for inclusion in the memoir.
 */
@Entity
@Table(name = "photo_asset")
public class PhotoAsset {

    public enum RestorationStatus {
        /** No restoration attempted yet. */
        ORIGINAL,
        /** Sent to the image-restoration service. */
        PROCESSING,
        /** Restoration complete. */
        RESTORED,
        /** Restoration was not needed or was declined. */
        SKIPPED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String originalFilename;

    /** Stored path or object-storage key. */
    @Column(nullable = false)
    private String storagePath;

    private String caption;

    /** Chapter or life-stage this photo belongs to. */
    private String chapterTag;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RestorationStatus restorationStatus = RestorationStatus.ORIGINAL;

    /** Path/key of the restored version, populated after processing. */
    private String restoredStoragePath;

    @Column(nullable = false)
    private Instant uploadedAt = Instant.now();

    // ── Getters & setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public String getChapterTag() { return chapterTag; }
    public void setChapterTag(String chapterTag) { this.chapterTag = chapterTag; }

    public RestorationStatus getRestorationStatus() { return restorationStatus; }
    public void setRestorationStatus(RestorationStatus restorationStatus) {
        this.restorationStatus = restorationStatus;
    }

    public String getRestoredStoragePath() { return restoredStoragePath; }
    public void setRestoredStoragePath(String restoredStoragePath) {
        this.restoredStoragePath = restoredStoragePath;
    }

    public Instant getUploadedAt() { return uploadedAt; }
}
