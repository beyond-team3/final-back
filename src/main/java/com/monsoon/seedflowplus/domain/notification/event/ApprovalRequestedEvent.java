package com.monsoon.seedflowplus.domain.notification.event;

import com.monsoon.seedflowplus.domain.deal.common.DealType;
import java.time.LocalDateTime;

public record ApprovalRequestedEvent(
        Long userId,
        Long approvalRequestId,
        DealType dealType,
        Long targetId,
        LocalDateTime occurredAt
) {
}
