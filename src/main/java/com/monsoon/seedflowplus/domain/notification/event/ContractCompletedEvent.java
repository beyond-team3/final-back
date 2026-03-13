package com.monsoon.seedflowplus.domain.notification.event;

import java.time.LocalDateTime;

public record ContractCompletedEvent(
        Long userId,
        Long contractId,
        String contractCode,
        String clientName,
        LocalDateTime occurredAt
) {
}
