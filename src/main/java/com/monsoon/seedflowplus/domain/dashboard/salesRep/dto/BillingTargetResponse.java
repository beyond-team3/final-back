package com.monsoon.seedflowplus.domain.dashboard.salesRep.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Vue: billings[] — 이번 달 청구 대상 계약 한 건
 *
 * docNo   → invoice_code          (예: "INV-2026-042")
 * client  → client_name
 * amount  → total_amount 포맷     (예: "1,500,000원")
 * status  → 상태 한글 레이블       (예: "청구 예정", "발행 완료")
 * type    → CSS 클래스용 식별자    ("pending" | "ready")
 *           DRAFT  → pending
 *           PUBLISHED → ready
 */
@Getter
@Builder
public class BillingTargetResponse {

    private String docNo;
    private String client;
    private String amount;
    private String status;
    private String type;
}