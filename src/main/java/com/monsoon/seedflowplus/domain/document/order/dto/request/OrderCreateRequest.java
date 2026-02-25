package com.monsoon.seedflowplus.domain.document.order.dto.request;

import lombok.Getter;

import java.util.List;

@Getter
public class OrderCreateRequest {

    private Long contractId;

    // 배송 정보
    private String shippingName;
    private String shippingPhone;
    private String shippingAddress;
    private String shippingAddressDetail;
    private String deliveryRequest;

    private List<OrderDetailRequest> items;
}
