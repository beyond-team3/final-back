package com.monsoon.seedflowplus.domain.deal.log.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseCreateEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@AttributeOverride(name = "id", column = @Column(name = "deal_log_detail_id"))
@Table(
        name = "tbl_sales_deal_log_detail",
        indexes = {
                @Index(name = "uk_deal_log_detail_deal_log_id", columnList = "deal_log_id", unique = true)
        }
)
public class DealLogDetail extends BaseCreateEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_log_id", nullable = false, unique = true)
    private SalesDealLog dealLog;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "diff_json", columnDefinition = "TEXT")
    private String diffJson;

    @Builder
    public DealLogDetail(SalesDealLog dealLog, String reason, String diffJson) {
        this.reason = reason;
        this.diffJson = diffJson;
        setDealLog(dealLog);
    }

    public void setDealLog(SalesDealLog dealLog) {
        SalesDealLog requiredDealLog = Objects.requireNonNull(dealLog, "dealLog는 null값이 될 수 없습니다.");
        if (this.dealLog != null && this.dealLog != requiredDealLog) {
            throw new IllegalStateException("DealLogDetail은 다른 dealLog로 재할당할 수 없습니다.");
        }
        if (requiredDealLog.getDetail() != null && requiredDealLog.getDetail() != this) {
            throw new IllegalStateException("이미 다른 DealLogDetail이 연결된 dealLog에는 재할당할 수 없습니다.");
        }
        this.dealLog = requiredDealLog;
        if (requiredDealLog.getDetail() != this) {
            requiredDealLog.setDetail(this);
        }
    }

    public void update(String reason, String diffJson) {
        this.reason = reason;
        this.diffJson = diffJson;
    }
}
