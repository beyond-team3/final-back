package com.monsoon.seedflowplus.domain.order.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseCreateEntity;
import com.monsoon.seedflowplus.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "tbl_order_header")
public class OrderHeader extends BaseCreateEntity {

    //@ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "contract_id")
    @Column(name = "contract_id", nullable = false)
    private Long contractId;        //private Contract contract;

    //@ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "client_id")
    @Column(name = "client_id", nullable = false)
    private Long clientId;      //private Client client;

    //@ManyToOne(fetch = FetchType.LAZY)
    //@JoinColumn(name = "employee_id")
    @Column
    private Long employeeId;    //private Employee employee;

    @Column
    private BigDecimal totalAmount;

    @Column
    private OrderStatus status;

}
