package com.monsoon.seedflowplus.domain.scoring.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tbl_account_score")
@AttributeOverride(name = "id", column = @Column(name = "account_score_id"))
public class AccountScore extends BaseModifyEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", unique = true, nullable = false)
    private Client client;

    @Column(nullable = false)
    private double totalScore;

    @Column(nullable = false)
    private double contractScore;

    @Column(nullable = false)
    private double orderScore;

    @Column(nullable = false)
    private double visitScore;

    @Column(length = 100)
    private String primaryReason;

    @Column(columnDefinition = "TEXT")
    private String detailDescription;

    @Builder
    public AccountScore(Client client, double totalScore, double contractScore, double orderScore, double visitScore, String primaryReason, String detailDescription) {
        this.client = client;
        this.totalScore = totalScore;
        this.contractScore = contractScore;
        this.orderScore = orderScore;
        this.visitScore = visitScore;
        this.primaryReason = primaryReason;
        this.detailDescription = detailDescription;
    }

    /**
     * 점수 정보를 업데이트합니다.
     */
    public void updateScore(double totalScore, double contractScore, double orderScore, double visitScore, String primaryReason, String detailDescription) {
        this.totalScore = totalScore;
        this.contractScore = contractScore;
        this.orderScore = orderScore;
        this.visitScore = visitScore;
        this.primaryReason = primaryReason;
        this.detailDescription = detailDescription;
    }
}
