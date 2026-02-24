package com.monsoon.seedflowplus.domain.order.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderDetailResponse {

    private Long orderDetailId;
    private Long contractDetailPk;
    private Long quantity;
}
