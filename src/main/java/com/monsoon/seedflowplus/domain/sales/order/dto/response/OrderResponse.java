package com.monsoon.seedflowplus.domain.sales.order.dto.response;

import com.monsoon.seedflowplus.domain.sales.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponse {

    private Long orderId;
    private String orderCode;
    private Long contractId;
    private Long clientId;
    private Long employeeId;

    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;

    // 배송 정보
    private String shippingName;
    private String shippingPhone;
    private String shippingAddress;
    private String shippingAddressDetail;
    private String deliveryRequest;

    private List<OrderDetailResponse> items;
}
