package com.monsoon.seedflowplus.domain.dashboard.sales.dto;


import lombok.Builder;
import lombok.Getter;

/**
 * Vue: monthlyBars[] — 바 차트 한 칸
 *
 * month   → "11월", "12월", ... "3월"
 * height  → 0~80 사이 정규화된 픽셀 높이 (최대값 기준 비례 계산)
 * current → 이번 달 여부 (CSS class 'current' 적용 여부)
 */
@Getter
@Builder
public class MonthlyBarResponse {

    private String month;   // 예: "3월"
    private int height;     // 0~80px 정규화
    private boolean current;
}