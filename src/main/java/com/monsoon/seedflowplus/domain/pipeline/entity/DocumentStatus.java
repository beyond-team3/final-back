package com.monsoon.seedflowplus.domain.pipeline.entity;

/**
 * 문서별 상태 Enum의 마커 인터페이스.
 * SalesHistory의 fromStatus / toStatus 필드는 String으로 저장되며,
 * 이 인터페이스를 구현한 enum의 name() 값만 허용된다.
 * pipelineType에 따라 허용되는 구현체는 서비스 레이어에서 검증한다.
 *
 * <pre>
 * PipelineType.RFQ  → RfqStatus
 * PipelineType.QUO  → QuotationStatus
 * PipelineType.CNT  → ContractStatus
 * PipelineType.ORD  → OrderStatus
 * PipelineType.STMT → StatementStatus
 * PipelineType.INV  → InvoiceStatus
 * PipelineType.PAY  → PaymentStatus
 * </pre>
 */
public interface DocumentStatus {
    String name();
}
