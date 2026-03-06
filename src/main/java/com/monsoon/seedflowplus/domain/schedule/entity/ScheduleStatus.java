package com.monsoon.seedflowplus.domain.schedule.entity;

public enum ScheduleStatus {
    ACTIVE,
    CANCELED,
    /**
     * Legacy value for backward compatibility with persisted rows.
     */
    @Deprecated
    DONE
}
