package com.goldenmemories.service.impl;

import com.goldenmemories.model.ApprovalRecord;
import com.goldenmemories.model.ApprovalRecord.Decision;
import com.goldenmemories.model.Project;
import com.goldenmemories.model.User;
import com.goldenmemories.repository.ApprovalRecordRepository;
import com.goldenmemories.repository.ProjectRepository;
import com.goldenmemories.service.ApprovalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ApprovalServiceImpl implements ApprovalService {

    private final ApprovalRecordRepository approvalRecordRepository;
    private final ProjectRepository projectRepository;

    public ApprovalServiceImpl(ApprovalRecordRepository approvalRecordRepository,
                                ProjectRepository projectRepository) {
        this.approvalRecordRepository = approvalRecordRepository;
        this.projectRepository = projectRepository;
    }

    @Override
    public ApprovalRecord submitDraft(Project project, String draftVersion, User submitter) {
        // Advance to DRAFT_REVIEW phase if not already there
        if (project.getCurrentPhase().ordinal() < Project.Phase.DRAFT_REVIEW.ordinal()) {
            project.setCurrentPhase(Project.Phase.DRAFT_REVIEW);
            projectRepository.save(project);
        }

        ApprovalRecord record = new ApprovalRecord();
        record.setProject(project);
        record.setDraftVersion(draftVersion);
        record.setReviewer(submitter);
        record.setDecision(Decision.PENDING);
        return approvalRecordRepository.save(record);
    }

    @Override
    public ApprovalRecord recordDecision(Long approvalId, Decision decision,
                                          String comments, User reviewer) {
        ApprovalRecord record = approvalRecordRepository.findById(approvalId)
            .orElseThrow(() -> new IllegalArgumentException("Approval record not found: " + approvalId));

        record.setDecision(decision);
        record.setComments(comments);
        record.setReviewer(reviewer);

        Project project = record.getProject();

        // Drive project phase based on decision
        switch (decision) {
            case APPROVED -> {
                project.setCurrentPhase(Project.Phase.PUBLISHING);
                projectRepository.save(project);
            }
            case REVISION_REQUESTED, REJECTED -> {
                project.setCurrentPhase(Project.Phase.EDITING);
                projectRepository.save(project);
            }
            default -> { /* PENDING — no phase change */ }
        }

        return approvalRecordRepository.save(record);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ApprovalRecord> latestApproval(Project project) {
        return approvalRecordRepository.findFirstByProjectOrderByCreatedAtDesc(project);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApprovalRecord> historyFor(Project project) {
        return approvalRecordRepository.findByProjectOrderByCreatedAtDesc(project);
    }
}
