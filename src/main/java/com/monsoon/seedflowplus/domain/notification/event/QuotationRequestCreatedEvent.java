package com.monsoon.seedflowplus.domain.notification.event;

import java.time.LocalDateTime;

public record QuotationRequestCreatedEvent(
        Long userId,
        Long quotationRequestId,
        String requestCode,
        String clientName,
        LocalDateTime occurredAt
) {
}
