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

    @Query("SELECT new com.monsoon.seedflowplus.domain.approval.dto.response.ReasonDto(r.targetId, d.reason) " +
           "FROM ApprovalDecision d " +
           "JOIN d.approvalStep s " +
           "JOIN s.approvalRequest r " +
           "WHERE r.dealType = :dealType AND r.targetId IN :targetIds " +
           "AND d.id IN (SELECT MAX(d2.id) FROM ApprovalDecision d2 " +
           "             JOIN d2.approvalStep s2 " +
           "             JOIN s2.approvalRequest r2 " +
           "             WHERE r2.dealType = :dealType AND r2.targetId = r.targetId)")
    java.util.List<com.monsoon.seedflowplus.domain.approval.dto.response.ReasonDto> findReasonsByTargets(
            @Param("dealType") DealType dealType, 
            @Param("targetIds") java.util.List<Long> targetIds);
}
