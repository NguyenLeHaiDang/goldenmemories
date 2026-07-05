package com.goldenmemories.model;

import jakarta.validation.constraints.NotNull;

public class ApprovalForm {

    @NotNull(message = "Please select a decision.")
    private ApprovalRecord.Decision decision;

    private String comments;

    public ApprovalRecord.Decision getDecision() { return decision; }
    public void setDecision(ApprovalRecord.Decision decision) { this.decision = decision; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}
