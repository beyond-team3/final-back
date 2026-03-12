package com.monsoon.seedflowplus.domain.sales.order.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseCreateEntity;
import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@NoArgsConstructor
@Entity
@AttributeOverride(name = "id", column = @Column(name = "order_id"))
@Table(name = "tbl_order_header")
public class OrderHeader extends BaseCreateEntity {

    @Column(name = "order_code", nullable = false, unique = true, length = 20)
    private String orderCode;   // ORD-20260223-001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private ContractHeader contract;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id", nullable = false)
    private SalesDeal deal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new CoreException(ErrorType.ORDER_ALREADY_CONFIRMED);
        }
        this.status = OrderStatus.CONFIRMED;
        this.deliveryDate = LocalDate.now().plusDays(3); // 주문 확정 시점 +3일 배송일

    }

    // 생성
    public static OrderHeader create(ContractHeader contract, Client client, SalesDeal deal, Employee employee, String orderCode) {
        OrderHeader order = new OrderHeader();
        order.orderCode = orderCode;
        order.contract = contract;   // ← contractId 대신 contract로
        order.client = client;
        order.deal = Objects.requireNonNull(deal, "deal must not be null");
        order.employee = employee;
        order.totalAmount = BigDecimal.ZERO;
        order.status = OrderStatus.PENDING;
        return order;
    }

    // 총액 업데이트
    public void updateTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELED;
    }

}
