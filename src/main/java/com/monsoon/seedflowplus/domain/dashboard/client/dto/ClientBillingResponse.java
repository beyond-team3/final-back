package com.monsoon.seedflowplus.domain.dashboard.client.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Vue: billings[] 한 건
 *
 * no     → invoice_code          "INV-2026-042"
 * due    → invoice_date 기반     "2026-03-31 마감"
 * amount → total_amount 포맷    "1,500,000원"
 * status → 상태 한글             "미결제" | "납부 완료"
 * type   → CSS 클래스
 *           invoice_date 7일 이내 → "due-soon"
 *           PUBLISHED            → "paid"
 *           그 외                 → ""
 */
@Getter
@Builder
public class ClientBillingResponse {
    private String no;
    private String due;
    private String amount;
    private String status;
    private String type;
}