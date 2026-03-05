package com.monsoon.seedflowplus.domain.dashboard.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Vue: salesTrendData.datasets[0].data (전년) / datasets[1].data (올해)
 * 단위: 만원, 미래 월은 null
 */
@Getter
@Builder
public class SalesTrendResponse {
    private List<Long> lastYear;
    private List<Long> thisYear;
}