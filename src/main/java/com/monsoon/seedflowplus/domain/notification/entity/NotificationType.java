package com.monsoon.seedflowplus.domain.notification.entity;

public enum NotificationType {
    ACCOUNT_ACTIVATED,              // 계정 활성화
    QUOTATION_REQUEST_CREATED,      // 견적요청서 생성
    QUOTATION_APPROVAL_RESULT,      // 견적서 승인 처리 완료(승인or반려)
    CONTRACT_APPROVAL_REQUESTED,    // 계약서 승인 요청
    CONTRACT_APPROVAL_RESULT,       // 계약서 승인 처리 완료
    CONTRACT_COMPLETED,             // 계약 체결
    INVOICE_ISSUED,                 // 청구서 발행
    STATEMENT_ISSUED,               // 명세서 발행
    STOCK_CHANGED,                  // 재고 변경
    CULTIVATION_SOWING_PROMOTION,   // 품종 추천(파종기 기반)
    CULTIVATION_HARVEST_FEEDBACK,   // 품종 피드백(수확기 기반)
    DEAL_STATUS_CHANGED,            // 딜 상태 변경
    APPROVAL_REQUESTED,             // 승인 요청
    APPROVAL_COMPLETED,             // 승인 완료
    APPROVAL_REJECTED               // 승인 반려
}
