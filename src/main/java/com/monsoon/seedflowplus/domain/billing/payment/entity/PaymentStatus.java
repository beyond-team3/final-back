package com.monsoon.seedflowplus.domain.billing.payment.entity;

import com.monsoon.seedflowplus.domain.deal.common.DocumentStatus;

public enum PaymentStatus implements DocumentStatus {
    PENDING,     // 결제 대기
    COMPLETED,   // 결제 완료
    FAILED       // 결제 실패
}
