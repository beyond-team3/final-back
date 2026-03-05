package com.monsoon.seedflowplus.domain.dashboard.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KpiResponse {
    /** 이번 달 전체 매출 포맷 — 예: "₩2.4억" */
    private String totalMonthlySales;
    /** 전년 동월 대비 증감률 — 예: "▲ 18.7%" */
    private String salesGrowthRate;
    /** 승인 대기 문서 건수 — 예: "23건" */
    private String pendingDocumentCount;
    /** 유형별 상세 — 예: "견적 12 / 계약 8 / 주문 3" */
    private String pendingDetail;
}