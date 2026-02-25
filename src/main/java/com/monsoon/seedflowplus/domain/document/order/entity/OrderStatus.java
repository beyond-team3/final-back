package com.monsoon.seedflowplus.domain.document.order.entity;

import com.monsoon.seedflowplus.domain.pipeline.enums.DocumentStatus;

public enum OrderStatus implements DocumentStatus {
    PENDING,    // 주문 대기
    CONFIRMED,  // 확정
    CANCELED    // 취소
}
