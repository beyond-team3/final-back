package com.monsoon.seedflowplus.domain.billing.statement.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseCreateEntity;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderHeader;
import jakarta.persistence.Index;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@AttributeOverride(name = "id", column = @Column(name = "statement_id"))
@Table(
        name = "tbl_statement",
        // 통계 전용 인덱스
        indexes = {
                @Index(name = "idx_statement_status_order", columnList = "status, order_id")
        }
)
public class Statement extends BaseCreateEntity {
    // id (Long) → BaseEntity에서 상속

    @Column(name = "statement_code", nullable = false, unique = true, length = 20)
    private String statementCode;   // STMT-20260223-001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderHeader orderHeader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id", nullable = false)
    private SalesDeal deal;

    @Column(name = "supply_amount")
    private BigDecimal supplyAmount;

    @Column(name = "vat_amount")
    private BigDecimal vatAmount;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatementStatus status;

    // 생성
    public static Statement create(OrderHeader orderHeader, SalesDeal deal, BigDecimal totalAmount, String statementCode) {
        Statement statement = new Statement();
        statement.statementCode = statementCode;
        statement.orderHeader = orderHeader;
        statement.deal = deal;
        statement.totalAmount = totalAmount;
        statement.supplyAmount = totalAmount.divide(BigDecimal.valueOf(1.1), 2, RoundingMode.HALF_UP);
        statement.vatAmount = totalAmount.subtract(statement.supplyAmount);
        statement.status = StatementStatus.ISSUED;
        return statement;
    }


    // 취소
    public void cancel() {
        this.status = StatementStatus.CANCELED;
    }
}
