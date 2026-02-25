package com.monsoon.seedflowplus.domain.sales.request.entity;

import com.monsoon.seedflowplus.domain.pipeline.entity.DocumentStatus;

public enum QuotationRequestStatus implements DocumentStatus {
    PENDING, // 대기
    REVIEWING, // 검토
    COMPLETED // 완료
}
