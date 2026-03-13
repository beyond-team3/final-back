package com.monsoon.seedflowplus.domain.approval.service;

import java.time.LocalDateTime;
import java.util.Objects;

public record OrderApprovalConfirmedEvent(
        Long orderId,
        Long approverUserId,
        LocalDateTime occurredAt
) {
    public OrderApprovalConfirmedEvent {
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
