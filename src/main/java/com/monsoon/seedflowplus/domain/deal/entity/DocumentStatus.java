package com.monsoon.seedflowplus.domain.deal.entity;

/**
 * 문서별 상태 Enum의 마커 인터페이스.
 * SalesDeal.currentStatus 및 SalesDealLog.fromStatus / toStatus 필드는 String으로 저장되며,
 * 이 인터페이스를 구현한 enum의 name() 값만 허용된다.
 * dealType에 따라 허용되는 구현체는 서비스 레이어에서 검증한다.
 *
 * <pre>
 * DealType.RFQ  → RfqStatus
 * DealType.QUO  → QuotationStatus
 * DealType.CNT  → ContractStatus
 * DealType.ORD  → OrderStatus
 * DealType.STMT → StatementStatus
 * DealType.INV  → InvoiceStatus
 * DealType.PAY  → PaymentStatus
 * </pre>
 */
public interface DocumentStatus {
    String name();
}
