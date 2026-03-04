package com.monsoon.seedflowplus.domain.notification.dto.response;

import com.monsoon.seedflowplus.domain.notification.entity.Notification;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationTargetType;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationType;
import java.time.LocalDateTime;

public record NotificationListItemResponse(
        Long id,
        NotificationType type,
        String title,
        String content,
        NotificationTargetType targetType,
        Long targetId,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {

    public static NotificationListItemResponse from(Notification notification) {
        return new NotificationListItemResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
