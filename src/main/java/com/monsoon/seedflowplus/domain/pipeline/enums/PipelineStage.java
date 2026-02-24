package com.monsoon.seedflowplus.domain.pipeline.enums;

/**
 * 문서별 status를 "공통 흐름 단계"로 추상화한 값.
 * SalesHistory.fromStage / toStage 필드에 사용된다.
 * 대시보드 필터·집계 시 pipelineType과 무관하게 공통 조건으로 사용할 수 있다.
 *
 * <pre>
 * 집계 시 유의사항:
 *   "최종 완료" 건수 = CONFIRMED + APPROVED 모두 포함해야 함
 *     - CONFIRMED: 주문 확정 (ORD)
 *     - APPROVED : 견적 최종 승인 (QUO) 또는 CONVERT 전 원본 문서 종결
 * </pre>
 *
 * 문서별 status → PipelineStage 매핑 예시:
 * <pre>
 * RFQ  RequestStatus.PENDING    → IN_PROGRESS
 *      RequestStatus.REVIEWING  → IN_PROGRESS
 *      RequestStatus.CANCELED   → CANCELED
 * QUO  QuotationStatus.DRAFT         → CREATED
 *      QuotationStatus.ADMIN_PENDING  → PENDING_ADMIN
 *      QuotationStatus.ADMIN_REJECTED → REJECTED_ADMIN
 *      QuotationStatus.CLIENT_PENDING → PENDING_CLIENT
 *      QuotationStatus.CLIENT_REJECTED→ REJECTED_CLIENT
 *      QuotationStatus.APPROVED       → APPROVED
 *      QuotationStatus.EXPIRED        → EXPIRED
 * CNT  ContractStatus.ADMIN_PENDING   → PENDING_ADMIN
 *      ContractStatus.ADMIN_REJECTED  → REJECTED_ADMIN
 *      ContractStatus.CLIENT_PENDING  → PENDING_CLIENT
 *      ContractStatus.CLIENT_REJECTED → REJECTED_CLIENT
 *      ContractStatus.COMPLETED       → APPROVED
 * ORD  OrderStatus.PENDING    → IN_PROGRESS
 *      OrderStatus.CONFIRMED  → CONFIRMED
 *      OrderStatus.CANCELED   → CANCELED
 * STMT StatementStatus.ISSUED   → ISSUED
 *      StatementStatus.CANCELED → CANCELED
 * INV  InvoiceStatus.DRAFT     → CREATED
 *      InvoiceStatus.PUBLISHED → ISSUED
 *      InvoiceStatus.PAID      → PAID
 *      InvoiceStatus.CANCELED  → CANCELED
 * PAY  PaymentStatus.PENDING   → IN_PROGRESS
 *      PaymentStatus.COMPLETED → PAID
 *      PaymentStatus.FAILED    → CANCELED
 * </pre>
 */
public enum PipelineStage {
    CREATED,          // 생성됨 (초안)
    IN_PROGRESS,      // 작성·검토 진행
    PENDING_ADMIN,    // 관리자 승인 대기
    REJECTED_ADMIN,   // 관리자 반려
    PENDING_CLIENT,   // 거래처 승인 대기
    REJECTED_CLIENT,  // 거래처 반려
    CONFIRMED,        // 확정 (주문 확정 등 - ORD)
    ISSUED,           // 발행됨 (STMT 발급, INV 발행 등)
    PAID,             // 결제 완료
    APPROVED,         // 최종 승인·완료 (QUO 최종 승인, CNT 계약 완료, CONVERT 전 종결)
    EXPIRED,          // 만료
    CANCELED          // 취소
}
