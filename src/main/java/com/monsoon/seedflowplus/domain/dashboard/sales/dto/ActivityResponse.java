package com.monsoon.seedflowplus.domain.dashboard.sales.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Vue: timeline[] — 영업 히스토리 한 건
 *
 * tbl_sales_history 기반 (actor_emp_id 기준, action_datetime DESC LIMIT 5)
 *
 * date   → action_datetime 포맷  (예: "2026-03-02")
 * title  → doc_type + action_type 조합 한글 (예: "견적 발송 - 그린팜")
 * detail → target_code + 부가 정보          (예: "QUO-2026-018")
 * state  → CSS 클래스용 식별자
 *           to_stage IN (COMPLETED, PAID, CONFIRMED, APPROVED) → "completed"
 *           그 외 → "pending"
 */
@Getter
@Builder
public class ActivityResponse {

    private String date;
    private String title;
    private String detail;
    private String state;
}