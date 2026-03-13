package com.monsoon.seedflowplus.domain.approval.repository;

import com.monsoon.seedflowplus.domain.approval.entity.ApprovalDecision;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalDecisionRepository extends JpaRepository<ApprovalDecision, Long> {

    boolean existsByApprovalStepId(Long approvalStepId);

    Optional<ApprovalDecision> findByApprovalStepId(Long approvalStepId);

    @Query("SELECT d.reason FROM ApprovalDecision d " +
           "JOIN d.approvalStep s " +
           "JOIN s.approvalRequest r " +
           "WHERE r.dealType = :dealType AND r.targetId = :targetId " +
           "ORDER BY d.decidedAt DESC")
    java.util.List<String> findReasonsByTarget(@Param("dealType") DealType dealType, @Param("targetId") Long targetId);
}
