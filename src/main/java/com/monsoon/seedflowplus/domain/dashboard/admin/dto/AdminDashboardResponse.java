package com.monsoon.seedflowplus.domain.dashboard.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * GET /api/dashboard/admin
 * Vue AdminDashboard.vue > dashboard.value 루트 구조
 */
@Getter
@Builder
public class AdminDashboardResponse {

    /** Vue: title */
    private String title;

    /** Vue: trendPeriod — 예: "2025년 – 2026년 월별 매출 추이" */
    private String trendPeriod;

    /**
     * Vue: prototypeKpis[] (더미 → 실데이터 전환)
     * 이번 달 매출 / 전년 동월 대비 증감률 / 승인 대기 문서
     */
    private KpiResponse kpis;

    /**
     * Vue: salesTrendData.datasets
     * 전년도 / 올해 월별 매출 (단위: 만원, 12개월 슬롯)
     */
    private SalesTrendResponse salesTrend;

    /**
     * Vue: rankings[]
     * rank / name / amount / width
     */
    private List<SalesRankingResponse> rankings;

    /** Vue: approvalCount */
    private int approvalCount;

    /**
     * Vue: approvals[]
     * item.title / item.meta / item.time
     */
    private List<ApprovalRequestResponse> approvals;
}