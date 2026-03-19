package com.monsoon.seedflowplus.domain.dashboard.client.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Vue: notifications[] 한 건
 *
 * tbl_notification 기반 (user_id 기준, created_at DESC LIMIT 10)
 *
 * time   → created_at 포맷  "2026-03-02"
 * title  → title 컬럼
 * detail → content 컬럼 (앞 50자)
 * isNew  → read_at IS NULL
 */
@Getter
@Builder
public class ClientNotificationResponse {
    private String time;
    private String title;
    private String detail;
    private boolean isNew;
    private String targetType;
    private String targetCode;
}