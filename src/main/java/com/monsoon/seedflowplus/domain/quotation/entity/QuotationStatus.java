package com.monsoon.seedflowplus.domain.quotation.entity;

import com.monsoon.seedflowplus.domain.pipeline.enums.DocumentStatus;

/**
 * 견적서(QUO) 상태.
 * 관리자 승인 → 거래처 승인 2단계 승인 구조.
 */
public enum QuotationStatus implements DocumentStatus {
    DRAFT,           // 작성 중         (영업사원 초안)
    ADMIN_PENDING,   // 관리자 승인 대기
    ADMIN_REJECTED,  // 관리자 반려
    CLIENT_PENDING,  // 거래처 승인 대기
    CLIENT_REJECTED, // 거래처 반려
    APPROVED,        // 최종 승인       (거래처 승인 완료 → CNT 전환 가능)
    EXPIRED          // 만료            (유효기간 초과)
}
