package com.monsoon.seedflowplus.domain.notification.event;

import java.time.LocalDateTime;

public record ProductCreatedEvent(
        Long userId,
        Long productId,
        String productCode,
        String productName,
        LocalDateTime occurredAt
) {
}
