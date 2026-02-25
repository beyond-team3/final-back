package com.monsoon.seedflowplus.domain.sales.order.dto.response;

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
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
}
