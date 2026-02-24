package com.monsoon.seedflowplus.domain.order.dto.response;

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
    private String status;
    private LocalDateTime createdAt;
}
