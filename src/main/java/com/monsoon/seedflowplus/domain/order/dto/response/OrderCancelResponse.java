package com.monsoon.seedflowplus.domain.order.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderCancelResponse {

    private Long orderId;
    private String status;
}
