package com.monsoon.seedflowplus.domain.dashboard.salesRep.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Vue: header.title / header.subtitle
 */
@Getter
@Builder
public class DashboardHeaderResponse {

    /** 예: "내 영업 현황" */
    private String title;

    /** 예: "2026년 3월 기준" */
    private String subtitle;
}