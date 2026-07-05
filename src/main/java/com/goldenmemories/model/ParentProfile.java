package com.goldenmemories.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * The parent whose life story is being captured.
 * Owned by one User (the adult child who registered the project).
 */
@Entity
@Table(name = "parent_profile")
public class ParentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The account that owns this parent profile. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    private User owner;

    @Column(nullable = false)
    private String parentName;

    /** e.g. "Mother", "Father", "Grandmother" */
    private String relation;

    private String zaloContact;

    private String additionalPhone;

    /** Free-text notes about the parent (health, language preference, etc.). */
    @Column(length = 1000)
    private String notes;

    /** Whether the parent has been successfully contacted and onboarded. */
    @Column(nullable = false)
    private boolean connected = false;

    // ── Getters & setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }

    public String getRelation() { return relation; }
    public void setRelation(String relation) { this.relation = relation; }

    public String getZaloContact() { return zaloContact; }
    public void setZaloContact(String zaloContact) { this.zaloContact = zaloContact; }

    public String getAdditionalPhone() { return additionalPhone; }
    public void setAdditionalPhone(String additionalPhone) { this.additionalPhone = additionalPhone; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }
}
