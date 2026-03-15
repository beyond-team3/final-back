package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.domain.notification.entity.NotificationType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CultivationNotificationCandidate {

    private final NotificationType type;
    private final Long userId;
    private final Long productId;
    private final String productName;
    private final Integer referenceMonth;
    private final Integer clientCount;
    private final LocalDateTime scheduledAt;
}
