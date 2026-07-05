package com.goldenmemories.repository;

import com.goldenmemories.model.ApprovalRecord;
import com.goldenmemories.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalRecordRepository extends JpaRepository<ApprovalRecord, Long> {

    List<ApprovalRecord> findByProjectOrderByCreatedAtDesc(Project project);

    Optional<ApprovalRecord> findFirstByProjectOrderByCreatedAtDesc(Project project);
}
