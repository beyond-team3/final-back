package com.monsoon.seedflowplus.domain.approval.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@AttributeOverride(name = "id", column = @Column(name = "approval_step_id"))
@Table(
        name = "tbl_approval_step",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_approval_step_request_order", columnNames = {"approval_request_id", "step_order"})
        }
)
public class ApprovalStep extends BaseModifyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_request_id", nullable = false)
    private ApprovalRequest approvalRequest;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 10)
    private ActorType actorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApprovalStepStatus status;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Builder
    public ApprovalStep(
            ApprovalRequest approvalRequest,
            int stepOrder,
            ActorType actorType,
            ApprovalStepStatus status,
            LocalDateTime decidedAt
    ) {
        this.approvalRequest = approvalRequest;
        this.stepOrder = stepOrder;
        this.actorType = actorType;
        this.status = status == null ? ApprovalStepStatus.WAITING : status;
        this.decidedAt = decidedAt;
    }

    public void setApprovalRequest(ApprovalRequest approvalRequest) {
        this.approvalRequest = approvalRequest;
    }

    public void approve(LocalDateTime decidedAt) {
        this.status = ApprovalStepStatus.APPROVED;
        this.decidedAt = decidedAt;
    }

    public void reject(LocalDateTime decidedAt) {
        this.status = ApprovalStepStatus.REJECTED;
        this.decidedAt = decidedAt;
    }
}
