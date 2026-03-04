package com.monsoon.seedflowplus.domain.approval.repository;

import com.monsoon.seedflowplus.domain.approval.entity.ApprovalDecision;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalDecisionRepository extends JpaRepository<ApprovalDecision, Long> {

    boolean existsByApprovalStepId(Long approvalStepId);

    Optional<ApprovalDecision> findByApprovalStepId(Long approvalStepId);
}
