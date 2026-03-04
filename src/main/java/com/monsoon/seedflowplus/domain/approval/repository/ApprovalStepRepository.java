package com.monsoon.seedflowplus.domain.approval.repository;

import com.monsoon.seedflowplus.domain.approval.entity.ApprovalStep;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {

    Optional<ApprovalStep> findByIdAndApprovalRequestId(Long id, Long approvalRequestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ApprovalStep s where s.id = :id and s.approvalRequest.id = :approvalRequestId")
    Optional<ApprovalStep> findByIdAndApprovalRequestIdForUpdate(
            @Param("id") Long id,
            @Param("approvalRequestId") Long approvalRequestId
    );

    Optional<ApprovalStep> findByApprovalRequestIdAndStepOrder(Long approvalRequestId, int stepOrder);

    List<ApprovalStep> findByApprovalRequestIdOrderByStepOrderAsc(Long approvalRequestId);
}
