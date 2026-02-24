package com.monsoon.seedflowplus.domain.order.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseCreateEntity;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Entity
@AttributeOverride(name = "id", column = @Column(name = "order_id"))
@Table(name = "tbl_order_header")
public class OrderHeader extends BaseCreateEntity {

    @Column(name = "order_code", nullable = false, unique = true, length = 20)
    private String orderCode;   // ORD-20260223-001

    @Column(name = "contract_id", nullable = false)
    private Long contractId;    // 타 파트라 ID만 저장

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", referencedColumnName = "id")
    private Employee employee;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    // 생성
    public static OrderHeader create(Long contractId, Client client, Employee employee, String orderCode) {
        OrderHeader order = new OrderHeader();
        order.orderCode = orderCode;
        order.contractId = contractId;
        order.client = client;
        order.employee = employee;
        order.totalAmount = BigDecimal.ZERO;
        order.status = OrderStatus.PENDING;
        return order;
    }

    // 총액 업데이트
    public void updateTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    // 상태 변경
    public void confirm() {
        this.status = OrderStatus.CONFIRMED;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELED;
    }

}
