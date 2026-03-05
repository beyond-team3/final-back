package com.monsoon.seedflowplus.domain.sales.order.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.List;

@Getter
public class OrderCreateRequest {

    @NotNull(message = "계약 헤더 ID는 필수입니다.")
    private Long headerId; // reason: 계약 헤더 PK임을 detailId와 명확히 구분하기 위해 contractId를 headerId로 명확화

    // 배송 정보
    private String shippingName;
    private String shippingPhone;
    private String shippingAddress;
    private String shippingAddressDetail;
    private String deliveryRequest;

    private List<OrderDetailRequest> items;
}
