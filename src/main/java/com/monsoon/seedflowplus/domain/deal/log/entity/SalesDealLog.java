package com.monsoon.seedflowplus.domain.deal.log.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseCreateEntity;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.common.DocumentStatusValidator;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@AttributeOverride(name = "id", column = @Column(name = "deal_log_id"))
@Table(
        name = "tbl_sales_deal_log",
        indexes = {
                @Index(name = "idx_deal_log_deal_dt", columnList = "deal_id, action_at"),
                @Index(name = "idx_deal_log_client_dt", columnList = "client_id, action_at"),
                @Index(name = "idx_deal_log_doc_ref_dt", columnList = "doc_type, ref_id, action_at")
        }
)
public class SalesDealLog extends BaseCreateEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id", nullable = false)
    private SalesDeal deal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 10)
    private DealType docType;

    @Column(name = "ref_id", nullable = false)
    private Long refId;

    @Column(name = "target_code", length = 30)
    private String targetCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_stage", length = 20)
    private DealStage fromStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_stage", length = 20)
    private DealStage toStage;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", length = 30)
    private String toStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 15)
    private ActionType actionType;

    @Column(name = "action_at", nullable = false)
    private LocalDateTime actionAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 10)
    private ActorType actorType;

    @Column(name = "actor_id")
    private Long actorId;

    @OneToOne(mappedBy = "dealLog", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private DealLogDetail detail;

    @Builder
    public SalesDealLog(
            SalesDeal deal,
            Client client,
            DealType docType,
            Long refId,
            String targetCode,
            DealStage fromStage,
            DealStage toStage,
            String fromStatus,
            String toStatus,
            ActionType actionType,
            LocalDateTime actionAt,
            ActorType actorType,
            Long actorId
    ) {
        SalesDeal requiredDeal = Objects.requireNonNull(deal, "deal은 null값이 될 수 없습니다.");
        Client dealClient = Objects.requireNonNull(requiredDeal.getClient(), "deal.client은 null값이 될 수 없습니다.");
        if (client != null && !Objects.equals(client, dealClient)) {
            throw new IllegalArgumentException("client는 deal.getClient()과 같아야 합니다.");
        }
        DocumentStatusValidator.validateNullable(docType, fromStatus, "fromStatus");
        DocumentStatusValidator.validateRequired(docType, toStatus, "toStatus");

        this.deal = requiredDeal;
        this.client = dealClient;
        this.docType = docType;
        this.refId = refId;
        this.targetCode = targetCode;
        this.fromStage = fromStage;
        this.toStage = toStage;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actionType = actionType;
        this.actionAt = actionAt;
        this.actorType = actorType;
        this.actorId = actorId;
    }

    public void setDetail(DealLogDetail detail) {
        this.detail = detail;
        if (detail != null && detail.getDealLog() != this) {
            detail.setDealLog(this);
        }
    }
}
