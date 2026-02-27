package com.monsoon.seedflowplus.domain.sales.quotation.entity;

import com.monsoon.seedflowplus.domain.deal.common.DocumentStatus;

public enum QuotationStatus implements DocumentStatus {
    WAITING_ADMIN, // 관리자 승인 대기
    REJECTED_ADMIN, // 관리자 반려 [재작성]
    WAITING_CLIENT, // 거래처 승인 대기
    REJECTED_CLIENT, // 거래처 반려 [재작성]
    FINAL_APPROVED, // 최종 승인
    WAITING_CONTRACT, // 계약진행중
    COMPLETED, // 완료
    EXPIRED // 만료
}
