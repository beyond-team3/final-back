package com.monsoon.seedflowplus.domain.sales.order.entity;


import com.monsoon.seedflowplus.core.common.entity.BaseEntity;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractDetail;
import jakarta.persistence.Index;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@AttributeOverride(name = "id", column = @Column(name = "order_detail_id"))
@Table(
        name = "tbl_order_detail",
        indexes = {
                @Index(name = "idx_order_detail_order_contract", columnList = "order_id, contract_detail_id")
        }
)
public class OrderDetail extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderHeader orderHeader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_detail_id", nullable = false)
    private ContractDetail contractDetail;

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
    public static OrderDetail create(OrderHeader orderHeader, ContractDetail contractDetail, Long quantity,
                                     String shippingName, String shippingPhone,
                                     String shippingAddress, String shippingAddressDetail,
                                     String deliveryRequest) {
        OrderDetail detail = new OrderDetail();
        detail.orderHeader = orderHeader;
        detail.contractDetail = contractDetail;   // ← contractDetailPk 대신 contractDetail로
        detail.quantity = quantity;
        detail.shippingName = shippingName;
        detail.shippingPhone = shippingPhone;
        detail.shippingAddress = shippingAddress;
        detail.shippingAddressDetail = shippingAddressDetail;
        detail.deliveryRequest = deliveryRequest;
        return detail;
    }
}
