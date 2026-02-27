package com.monsoon.seedflowplus.domain.notification.repository;

import com.monsoon.seedflowplus.domain.notification.entity.Notification;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationTargetType;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationType;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByUser_IdAndTypeAndTargetTypeAndTargetIdAndCreatedAtBetween(
            Long userId,
            NotificationType type,
            NotificationTargetType targetType,
            Long targetId,
            LocalDateTime from,
            LocalDateTime to
    );
}
