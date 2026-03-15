package com.monsoon.seedflowplus.domain.approval.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseCreateEntity;
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
@AttributeOverride(name = "id", column = @Column(name = "approval_decision_id"))
@Table(
        name = "tbl_approval_decision",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_approval_decision_step", columnNames = {"approval_step_id"})
        }
)
public class ApprovalDecision extends BaseCreateEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_step_id", nullable = false)
    private ApprovalStep approvalStep;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 10)
    private DecisionType decision;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "decided_by_user_id", nullable = false)
    private Long decidedByUserId;

    @Column(name = "decided_at", nullable = false)
    private LocalDateTime decidedAt;

    @Builder
    public ApprovalDecision(
            ApprovalStep approvalStep,
            DecisionType decision,
            String reason,
            Long decidedByUserId,
            LocalDateTime decidedAt
    ) {
        this.approvalStep = approvalStep;
        this.decision = decision;
        this.reason = reason;
        this.decidedByUserId = decidedByUserId;
        this.decidedAt = decidedAt;
    }
}
