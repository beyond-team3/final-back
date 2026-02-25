package com.monsoon.seedflowplus.domain.sales.order.dto.response;

import com.monsoon.seedflowplus.domain.sales.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderCancelResponse {

    private Long orderId;
    private OrderStatus status;
}
