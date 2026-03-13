package com.monsoon.seedflowplus.domain.sales.order.dto.response;

import com.monsoon.seedflowplus.domain.sales.order.entity.OrderHeader;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class OrderListResponse {

    private Long orderId;
    private String orderCode;
    private Long headerId;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;


    private OrderListResponse toOrderListResponse(OrderHeader orderHeader) {
        return OrderListResponse.builder()
                .orderId(orderHeader.getId())
                .orderCode(orderHeader.getOrderCode())
                .headerId(orderHeader.getContract().getId())  // ← 추가
                .totalAmount(orderHeader.getTotalAmount())
                .status(orderHeader.getStatus())
                .createdAt(orderHeader.getCreatedAt())
                .build();
    }
}


