package com.monsoon.seedflowplus.domain.schedule.dto.command;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
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
    private static final int TITLE_MAX_LENGTH = 200;
    private static final int EXTERNAL_KEY_MAX_LENGTH = 180;

    public DealScheduleUpsertCommand {
        externalKey = externalKey == null ? null : externalKey.trim();
        title = title == null ? null : title.trim();

        if (externalKey == null || externalKey.isBlank()) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (externalKey.length() > EXTERNAL_KEY_MAX_LENGTH) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (dealId == null || dealId <= 0) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (clientId == null || clientId <= 0) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (assigneeUserId == null || assigneeUserId <= 0) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (eventType == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (docType == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (title == null || title.isBlank()) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (title.length() > TITLE_MAX_LENGTH) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (startAt == null || endAt == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (!startAt.isBefore(endAt)) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (lastSyncedAt == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
    }
}
