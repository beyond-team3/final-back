package com.monsoon.seedflowplus.domain.sales.quotation.entity;

import com.monsoon.seedflowplus.domain.deal.common.DocumentStatus;

public enum QuotationStatus implements DocumentStatus {
    WAITING_ADMIN, // 관리자 승인 대기
    REJECTED_ADMIN, // 관리자 반려 [재작성]
    WAITING_CLIENT, // 거래처 승인 대기
    REJECTED_CLIENT, // 거래처 반려 [재작성]
    FINAL_APPROVED, // 최종  >> 견적서 체결
    WAITING_CONTRACT, // 계약서 작성 및 승인 프로세스 진행 중
    COMPLETED, // 완료 >> 계약성사
    EXPIRED, // 만료 >> 계약성사XX 그냥 만료
    DELETED // 삭제
}
