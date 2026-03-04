package com.monsoon.seedflowplus.domain.dashboard.admin.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Vue: approvals[].title / meta / time
 * tbl_sales_deal_log 기반 (to_stage = PENDING_ADMIN, 최근 10건)
 */
@Getter
@Builder
public class ApprovalRequestResponse {
    /** 예: "견적 승인 요청 - 그린팜" */
    private String title;
    /** 요청자 이름 */
    private String meta;
    /** 요청일 "2026-03-02" */
    private String time;
}