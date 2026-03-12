package com.monsoon.seedflowplus.domain.notification.event;

import java.time.LocalDateTime;

public record InvoiceIssuedEvent(
        Long userId,
        Long invoiceId,
        String invoiceCode,
        String clientName,
        LocalDateTime occurredAt
) {
}
