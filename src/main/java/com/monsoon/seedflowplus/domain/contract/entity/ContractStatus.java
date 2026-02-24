package com.monsoon.seedflowplus.domain.contract.entity;

import com.monsoon.seedflowplus.domain.pipeline.enums.DocumentStatus;

/**
 * 계약서(CNT) 상태.
 * QuotationStatus와 대칭되도록 반려를 ADMIN_REJECTED / CLIENT_REJECTED로 구분한다.
 * PipelineStage.REJECTED_ADMIN / REJECTED_CLIENT 매핑을 위해 반드시 구분이 필요하다.
 *
 * 최종 완료(COMPLETED)는 PipelineStage.APPROVED로 매핑된다.
 */
public enum ContractStatus implements DocumentStatus {
    ADMIN_PENDING,   // 관리자 서명 대기
    ADMIN_REJECTED,  // 관리자 반려
    CLIENT_PENDING,  // 거래처 서명 대기
    CLIENT_REJECTED, // 거래처 반려
    COMPLETED        // 계약 완료 (양측 서명 완료)
}
