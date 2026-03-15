package com.monsoon.seedflowplus.domain.deal.core.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.common.DocumentStatusValidator;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@AttributeOverride(name = "id", column = @Column(name = "deal_id"))
@Table(
        name = "tbl_sales_deal",
        indexes = {
                @Index(name = "idx_deal_client_open_activity", columnList = "client_id, closed_at, last_activity_at"),
                @Index(name = "idx_deal_owner_activity", columnList = "owner_emp_id, last_activity_at"),
                @Index(name = "idx_deal_stage_activity", columnList = "current_stage, last_activity_at")
        }
)
public class SalesDeal extends BaseModifyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_emp_id", nullable = false)
    private Employee ownerEmp;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_stage", nullable = false, length = 20)
    private DealStage currentStage;

    @Column(name = "current_status", nullable = false, length = 30)
    private String currentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "latest_doc_type", nullable = false, length = 10)
    private DealType latestDocType;

    @Column(name = "latest_ref_id", nullable = false)
    private Long latestRefId;

    @Column(name = "latest_target_code", length = 30)
    private String latestTargetCode;

    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "summary_memo", length = 500)
    private String summaryMemo;

    @Builder
    public SalesDeal(
            Client client,
            Employee ownerEmp,
            DealStage currentStage,
            String currentStatus,
            DealType latestDocType,
            Long latestRefId,
            String latestTargetCode,
            LocalDateTime lastActivityAt,
            LocalDateTime closedAt,
            String summaryMemo
    ) {
        DocumentStatusValidator.validateRequired(latestDocType, currentStatus, "currentStatus");
        this.client = client;
        this.ownerEmp = ownerEmp;
        this.currentStage = currentStage;
        this.currentStatus = currentStatus;
        this.latestDocType = latestDocType;
        this.latestRefId = latestRefId;
        this.latestTargetCode = latestTargetCode;
        this.lastActivityAt = lastActivityAt;
        this.closedAt = closedAt;
        this.summaryMemo = summaryMemo;
    }

    public void updateSnapshot(
            DealStage currentStage,
            String currentStatus,
            DealType latestDocType,
            Long latestRefId,
            String latestTargetCode,
            LocalDateTime lastActivityAt
    ) {
        DocumentStatusValidator.validateRequired(latestDocType, currentStatus, "currentStatus");
        this.currentStage = currentStage;
        this.currentStatus = currentStatus;
        this.latestDocType = latestDocType;
        this.latestRefId = latestRefId;
        this.latestTargetCode = latestTargetCode;
        this.lastActivityAt = lastActivityAt;
    }

    public void updateOwner(Employee ownerEmp) {
        this.ownerEmp = ownerEmp;
    }

    public void updateSummaryMemo(String summaryMemo) {
        this.summaryMemo = summaryMemo;
    }

    public void close(LocalDateTime closedAt) {
        if (this.closedAt != null) {
            return;
        }
        this.closedAt = closedAt;
    }

    public void reopen() {
        this.closedAt = null;
    }
}
