package com.monsoon.seedflowplus.domain.billing.invoice.entity;

import com.monsoon.seedflowplus.domain.pipeline.entity.DocumentStatus;

public enum InvoiceStatus implements DocumentStatus {
    DRAFT,       // 자동 생성된 초안
    PUBLISHED,   // 영업사원 발행 완료
    PAID,        // 결제 완료
    CANCELED     // 취소
}
