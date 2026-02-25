package com.monsoon.seedflowplus.domain.document.order.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderDetailResponse {

    private Long orderDetailId;
    private Long contractDetailId;
    private Long quantity;
}
