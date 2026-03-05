package com.monsoon.seedflowplus.domain.dashboard.client.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Vue: billingCycle.value / billingCycle.next
 *
 * value → "월별 청구" | "분기별 청구" | "반기별 청구"
 * next  → "다음 청구일 2026-04-01"
 */
@Getter
@Builder
public class BillingCycleResponse {
    private String value;
    private String next;
}