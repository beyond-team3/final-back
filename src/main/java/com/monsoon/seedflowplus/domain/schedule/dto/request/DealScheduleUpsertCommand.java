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
}
