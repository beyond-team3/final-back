package com.monsoon.seedflowplus.domain.notification.event;

import java.time.LocalDateTime;

public record StatementIssuedEvent(
        Long userId,
        Long statementId,
        String statementCode,
        String orderCode,
        LocalDateTime occurredAt
) {
}
