package com.goldenmemories.model;

import jakarta.validation.constraints.NotBlank;

public class ParentProfileForm {

    @NotBlank(message = "Parent's name is required.")
    private String parentName;

    @NotBlank(message = "Please specify the relation (e.g. Mother, Father).")
    private String relation;

    private String zaloContact;

    private String additionalPhone;

    private String notes;

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
}
