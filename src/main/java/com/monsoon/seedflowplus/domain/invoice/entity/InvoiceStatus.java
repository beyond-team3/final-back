package com.monsoon.seedflowplus.domain.invoice.entity;

public enum InvoiceStatus {
    DRAFT,       // 자동 생성된 초안
    PUBLISHED,   // 영업사원 발행 완료
    PAID,        // 결제 완료
    CANCELED     // 취소
}
