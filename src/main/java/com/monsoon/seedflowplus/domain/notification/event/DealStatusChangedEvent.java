package com.monsoon.seedflowplus.domain.notification.event;

import java.time.LocalDateTime;

public record DealStatusChangedEvent(
        Long userId,
        Long dealId,
        String fromStatus,
        String toStatus,
        LocalDateTime occurredAt
) {
}
