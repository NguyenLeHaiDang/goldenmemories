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

    // ── Phase 4: Print handoff ────────────────────────────────────────────────
    private String printVendorName;
    private String printDeliveryAddress;
    @Column(length = 2000)
    private String printNotes;

    // ── Phase 4: Cloud archive ────────────────────────────────────────────────
    private String archiveUrl;
    private String archiveProvider;
    @Column(length = 2000)
    private String archiveNotes;

    // Payment state for the publishing handoff
    @Column(nullable = false)
    private boolean paymentConfirmed = false;
    private String paymentGateway;
    private String paymentMethod;
    private String paymentReference;
    private String paymentTransactionNo;
    private String paymentResponseCode;
    private String paymentBankCode;
    private Instant paymentConfirmedAt;

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

    // ── Print handoff getters/setters ─────────────────────────────────────────
    public String getPrintVendorName() { return printVendorName; }
    public void setPrintVendorName(String printVendorName) { this.printVendorName = printVendorName; }

    public String getPrintDeliveryAddress() { return printDeliveryAddress; }
    public void setPrintDeliveryAddress(String printDeliveryAddress) { this.printDeliveryAddress = printDeliveryAddress; }

    public String getPrintNotes() { return printNotes; }
    public void setPrintNotes(String printNotes) { this.printNotes = printNotes; }

    // ── Archive getters/setters ───────────────────────────────────────────────
    public String getArchiveUrl() { return archiveUrl; }
    public void setArchiveUrl(String archiveUrl) { this.archiveUrl = archiveUrl; }

    public String getArchiveProvider() { return archiveProvider; }
    public void setArchiveProvider(String archiveProvider) { this.archiveProvider = archiveProvider; }

    public String getArchiveNotes() { return archiveNotes; }
    public void setArchiveNotes(String archiveNotes) { this.archiveNotes = archiveNotes; }

    public boolean isPaymentConfirmed() { return paymentConfirmed; }
    public void setPaymentConfirmed(boolean paymentConfirmed) { this.paymentConfirmed = paymentConfirmed; }

    public String getPaymentGateway() { return paymentGateway; }
    public void setPaymentGateway(String paymentGateway) { this.paymentGateway = paymentGateway; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public String getPaymentTransactionNo() { return paymentTransactionNo; }
    public void setPaymentTransactionNo(String paymentTransactionNo) { this.paymentTransactionNo = paymentTransactionNo; }

    public String getPaymentResponseCode() { return paymentResponseCode; }
    public void setPaymentResponseCode(String paymentResponseCode) { this.paymentResponseCode = paymentResponseCode; }

    public String getPaymentBankCode() { return paymentBankCode; }
    public void setPaymentBankCode(String paymentBankCode) { this.paymentBankCode = paymentBankCode; }

    public Instant getPaymentConfirmedAt() { return paymentConfirmedAt; }
    public void setPaymentConfirmedAt(Instant paymentConfirmedAt) { this.paymentConfirmedAt = paymentConfirmedAt; }
}
