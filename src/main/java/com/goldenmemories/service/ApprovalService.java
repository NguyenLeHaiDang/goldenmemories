package com.goldenmemories.service;

import com.goldenmemories.model.ApprovalRecord;
import com.goldenmemories.model.ApprovalRecord.Decision;
import com.goldenmemories.model.Project;
import com.goldenmemories.model.User;

import java.util.List;
import java.util.Optional;

public interface ApprovalService {

    /**
     * Submit the current memoir draft for review.
     * Creates a new ApprovalRecord with PENDING status and advances the
     * project to DRAFT_REVIEW phase if not already there.
     */
    ApprovalRecord submitDraft(Project project, String draftVersion, User submitter);

    /**
     * Record the reviewer's decision on a pending draft.
     * If APPROVED, advances the project to PUBLISHING phase.
     * If REVISION_REQUESTED, moves project back to EDITING phase.
     */
    ApprovalRecord recordDecision(Long approvalId, Decision decision,
                                  String comments, User reviewer);

    /** Most recent approval record for a project. */
    Optional<ApprovalRecord> latestApproval(Project project);

    /** Full approval history for a project. */
    List<ApprovalRecord> historyFor(Project project);
}
