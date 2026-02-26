package com.monsoon.seedflowplus.domain.deal.common;

/**
 * 딜 로그 추적 대상 문서 종류.
 * SalesDealLog.docType, SalesDeal.latestDocType 필드에 사용된다.
 * PAY는 결제를 독립 Deal로 추적할 경우에만 사용한다.
 */
public enum DealType {
    RFQ,   // 견적요청서 (Request for Quotation)
    QUO,   // 견적서     (Quotation)
    CNT,   // 계약서     (Contract)
    ORD,   // 주문서     (Order)
    STMT,  // 명세서     (Statement)
    INV,   // 청구서     (Invoice)
    PAY    // 결제       (Payment) - 결제를 독립 Deal로 추적할 때만 사용
}
