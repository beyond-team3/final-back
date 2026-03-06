package com.monsoon.seedflowplus.domain.schedule.entity;

public enum ScheduleVisibility {
    PRIVATE,
    PUBLIC,
    /**
     * Legacy value for backward compatibility with persisted rows.
     */
    @Deprecated
    TEAM
}
