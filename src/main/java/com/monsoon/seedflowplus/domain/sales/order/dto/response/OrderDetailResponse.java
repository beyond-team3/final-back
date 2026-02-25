package com.monsoon.seedflowplus.domain.sales.order.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderDetailResponse {

    private Long orderDetailId;
    private Long contractDetailId;
    private Long quantity;
}
