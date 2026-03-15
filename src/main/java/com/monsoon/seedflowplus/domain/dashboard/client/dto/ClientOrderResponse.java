package com.monsoon.seedflowplus.domain.dashboard.client.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Vue: orders[] 한 건
 *
 * no          → order_code          "ORD-2026-031"
 * date        → created_at 포맷     "2026-02-13"
 * status      → 상태 한글            "처리 중" | "대기" | "완료"
 * statusClass → CSS 클래스          "processing" | "pending" | "completed"
 * summary     → 주문 품목 요약       "토마토 씨앗 외 2건"
 * amount      → total_amount 포맷   "540,000원"
 * action      → 고정 문자열          "상세 보기"
 *
 * tbl_order_header status → statusClass 매핑
 *   PENDING   → pending     "대기"
 *   CONFIRMED → processing  "처리 중"
 *   CANCELED  → completed   "완료" (취소도 종결 처리)
 */
@Getter
@Builder
public class ClientOrderResponse {
    private String no;
    private String date;
    private String status;
    private String statusClass;
    private String summary;
    private String amount;
    private String action;
}