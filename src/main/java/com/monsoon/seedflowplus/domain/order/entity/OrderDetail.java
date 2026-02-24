package com.monsoon.seedflowplus.domain.order.entity;


import com.monsoon.seedflowplus.core.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@AttributeOverride(name = "id", column = @Column(name = "order_detail_id"))
@Table(name = "tbl_order_detail")
public class OrderDetail extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderHeader orderHeader;

    @Column(name = "contract_detail_pk", nullable = false)
    private Long contractDetailPk;   // 타 파트라 ID만 저장

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    // 배송정보(임의로 주문 엔터티에 넣어둠)
    @Column(name = "shipping_name", length = 50)
    private String shippingName;  // 수령인

    @Column(name = "shipping_phone", length = 20)
    private String shippingPhone; // 연락처

    @Column(name = "shipping_address", length = 255)
    private String shippingAddress; // 기본 주소

    @Column(name = "shipping_address_detail", length = 255)
    private String shippingAddressDetail; // 상세 주소

    @Column(name = "delivery_request", length = 255)
    private String deliveryRequest; // 요청사항


    // 생성
    public static OrderDetail create(OrderHeader orderHeader, Long contractDetailPk, Long quantity) {
        OrderDetail detail = new OrderDetail();
        detail.orderHeader = orderHeader;
        detail.contractDetailPk = contractDetailPk;
        detail.quantity = quantity;
        return detail;
    }
}
