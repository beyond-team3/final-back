package com.monsoon.seedflowplus.domain.notification.event;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import java.time.LocalDateTime;

public record AccountActivatedEvent(
        Long userId,
        Role role,
        LocalDateTime occurredAt
) {
}
