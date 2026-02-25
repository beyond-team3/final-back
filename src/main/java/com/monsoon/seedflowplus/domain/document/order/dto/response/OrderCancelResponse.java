package com.monsoon.seedflowplus.domain.document.order.dto.response;

import com.monsoon.seedflowplus.domain.document.order.entity.OrderStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderCancelResponse {

    private Long orderId;
    private OrderStatus status;
}
