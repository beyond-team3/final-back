package com.monsoon.seedflowplus.domain.approval.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@AttributeOverride(name = "id", column = @Column(name = "approval_request_id"))
@Table(
        name = "tbl_approval_request",
        indexes = {
                @Index(name = "idx_approval_request_deal_target", columnList = "deal_type, target_id"),
                @Index(name = "idx_approval_request_status", columnList = "status")
        }
)
public class ApprovalRequest extends BaseModifyEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "deal_type", nullable = false, length = 10)
    private DealType dealType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApprovalStatus status;

    @Column(name = "client_id_snapshot")
    private Long clientIdSnapshot;

    @Column(name = "target_code_snapshot", length = 30)
    private String targetCodeSnapshot;

    @OneToMany(mappedBy = "approvalRequest", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApprovalStep> steps = new ArrayList<>();

    @Builder
    public ApprovalRequest(
            DealType dealType,
            Long targetId,
            ApprovalStatus status,
            Long clientIdSnapshot,
            String targetCodeSnapshot
    ) {
        this.dealType = dealType;
        this.targetId = targetId;
        this.status = status == null ? ApprovalStatus.PENDING : status;
        this.clientIdSnapshot = clientIdSnapshot;
        this.targetCodeSnapshot = targetCodeSnapshot;
    }

    public void addStep(ApprovalStep step) {
        this.steps.add(step);
        if (step.getApprovalRequest() != this) {
            step.setApprovalRequest(this);
        }
    }

    public void approve() {
        this.status = ApprovalStatus.APPROVED;
    }

    public void reject() {
        this.status = ApprovalStatus.REJECTED;
    }

    public void cancel() {
        this.status = ApprovalStatus.CANCELED;
    }
}
