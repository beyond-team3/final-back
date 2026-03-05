package com.monsoon.seedflowplus.domain.dashboard.admin.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Vue: rankings[].rank / name / amount / width
 */
@Getter
@Builder
public class SalesRankingResponse {
    private int rank;
    private String name;
    private String amount;  // "₩45,600,000"
    private int width;      // 1위=100, 나머지 비례
}