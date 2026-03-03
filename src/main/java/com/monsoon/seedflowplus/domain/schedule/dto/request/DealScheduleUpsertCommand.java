package com.monsoon.seedflowplus.domain.schedule.dto.request;

import com.monsoon.seedflowplus.domain.schedule.entity.DealDocType;
import com.monsoon.seedflowplus.domain.schedule.entity.DealScheduleEventType;
import java.time.LocalDateTime;

public record DealScheduleUpsertCommand(
        String externalKey,
        Long dealId,
        Long clientId,
        Long assigneeUserId,
        DealScheduleEventType eventType,
        DealDocType docType,
        Long refDocId,
        Long refDealLogId,
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime lastSyncedAt
) {
    public DealScheduleUpsertCommand {
        if (externalKey == null || externalKey.isBlank()) {
            throw new IllegalArgumentException("externalKey must not be blank");
        }
        if (dealId == null || dealId <= 0) {
            throw new IllegalArgumentException("dealId must be positive");
        }
        if (clientId == null || clientId <= 0) {
            throw new IllegalArgumentException("clientId must be positive");
        }
        if (assigneeUserId == null || assigneeUserId <= 0) {
            throw new IllegalArgumentException("assigneeUserId must be positive");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("eventType must not be null");
        }
        if (docType == null) {
            throw new IllegalArgumentException("docType must not be null");
        }
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("startAt/endAt must not be null");
        }
        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("startAt must be before endAt");
        }
        if (lastSyncedAt == null) {
            throw new IllegalArgumentException("lastSyncedAt must not be null");
        }
    }
}
