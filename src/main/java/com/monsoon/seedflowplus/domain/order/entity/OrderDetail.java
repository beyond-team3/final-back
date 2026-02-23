package com.monsoon.seedflowplus.domain.order.entity;


import com.monsoon.seedflowplus.core.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "tbl_order_detail")
public class OrderDetail extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "contract_detail_pk", nullable = false)
    private Long contractDetailPk;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "quantity", nullable = false)
    private Long quantity;
}
