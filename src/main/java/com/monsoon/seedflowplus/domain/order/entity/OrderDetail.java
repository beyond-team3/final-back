package com.monsoon.seedflowplus.domain.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "tbl_order_header")
public class OrderDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

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

    /*public static OrderHeader create(Long)*/
}
