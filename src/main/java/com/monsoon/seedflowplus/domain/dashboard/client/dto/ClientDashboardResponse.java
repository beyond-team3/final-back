package com.monsoon.seedflowplus.domain.dashboard.client.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * GET /api/dashboard/client
 * Vue ClientDashboard.vue > dashboard.value 루트 구조
 */
@Getter
@Builder
public class ClientDashboardResponse {

    /** Vue: title */
    private String title;

    /** Vue: subtitle */
    private String subtitle;

    /**
     * Vue: billingCycle.value / billingCycle.next
     * 청구 사이클 정보
     */
    private BillingCycleResponse billingCycle;

    /**
     * Vue: orders[]
     * 최근 주문 내역 (order.no / date / status / statusClass / summary / amount / action)
     */
    private List<ClientOrderResponse> orders;

    /**
     * Vue: billings[]
     * 미결제 청구서 목록 (billing.no / due / amount / status / type)
     */
    private List<ClientBillingResponse> billings;

    /**
     * Vue: notifications[]
     * 최근 알림 (item.time / title / detail / isNew)
     */
    private List<ClientNotificationResponse> notifications;
}