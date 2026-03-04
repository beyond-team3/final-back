package com.monsoon.seedflowplus.domain.dashboard.sales.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Vue: monthlySales.*
 *
 * 모든 필드는 Vue가 바로 렌더링할 수 있도록 포맷된 문자열로 전달한다.
 *
 * periodLabel    → "2026년 3월"
 * amount         → "3,560,000원"
 * change         → "▼ 15.2%" | "▲ 8.3%" | "-"   (증감률, 방향 기호 포함)
 * diff           → "-640,000원" | "+320,000원"    (증감 금액)
 * completedCount → "6건"
 */
@Getter
@Builder
public class MonthlySalesResponse {

    /** 조회 기준 월 레이블 */
    private String periodLabel;

    /** 이번 달 매출 총액 (PAID 기준) */
    private String amount;

    /** 전월 대비 증감률 (방향 기호 포함 문자열) */
    private String change;

    /** 전월 대비 증감 금액 문자열 */
    private String diff;

    /** 이번 달 완료 계약 건수 */
    private String completedCount;
}