package com.monsoon.seedflowplus.domain.dashboard.salesRep.dto;

import com.monsoon.seedflowplus.domain.scoring.dto.AccountPriorityResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * GET /api/dashboard/sales
 * Vue SalesRepDashboard.vue > dashboard.value 루트 구조
 */
@Getter
@Builder
public class SalesDashboardResponse {

    /** Vue: dashboard.value.header.title / header.subtitle */
    private DashboardHeaderResponse header;

    /** Vue: dashboard.value.monthlySales.* */
    private MonthlySalesResponse monthlySales;

    /**
     * Vue: dashboard.value.monthlyBars[]
     * 바 차트용 최근 6개월 데이터 (bar.month / bar.height / bar.current)
     */
    private List<MonthlyBarResponse> monthlyBars;

    /**
     * Vue: dashboard.value.billings[]
     * 이번 달 청구 대상 계약 (item.docNo / item.client / item.amount / item.status / item.type)
     */
    private List<BillingTargetResponse> billings;

    /**
     * Vue: dashboard.value.timeline[]
     * 최근 영업 히스토리 5건 (item.date / item.title / item.detail / item.state)
     */
    private List<ActivityResponse> timeline;

    /**
     * Vue: dashboard.value.priorityAccounts[]
     * 우선 연락 필요 거래처 스코어링 목록
     */
    private List<AccountPriorityResponse> priorityAccounts;

    /**
     * Vue: dashboard.value.timelineFilters[]
     * 타임라인 필터 탭 — 고정값 ['전체', '견적', '계약', '주문']
     */
    private List<String> timelineFilters;
}