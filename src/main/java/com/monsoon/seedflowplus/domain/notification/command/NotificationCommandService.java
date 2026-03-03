package com.monsoon.seedflowplus.domain.notification.command;

import com.monsoon.seedflowplus.domain.notification.repository.NotificationRepository;
import com.monsoon.seedflowplus.domain.notification.service.CultivationNotificationService;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final CultivationNotificationService cultivationNotificationService;

    public void markAsRead(Long userId, Long notificationId, LocalDateTime now) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(notificationId, "notificationId must not be null");
        Objects.requireNonNull(now, "now must not be null");

        notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .ifPresent(notification -> notification.markAsRead(now));
    }

    public void markAllAsRead(Long userId, LocalDateTime now) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(now, "now must not be null");

        notificationRepository.markAllAsRead(userId, now);
    }

    public void createCultivationSowingPromotion(
            Long userId,
            Long productId,
            LocalDateTime sowingStart,
            LocalDateTime sowingEnd,
            LocalDateTime now
    ) {
        cultivationNotificationService.createSowingPromotionNotification(
                userId,
                productId,
                sowingStart,
                sowingEnd,
                now
        );
    }

    public void createCultivationHarvestFeedback(
            Long userId,
            Long productId,
            LocalDateTime harvestingStart,
            LocalDateTime harvestingEnd,
            LocalDateTime now
    ) {
        cultivationNotificationService.createHarvestFeedbackNotification(
                userId,
                productId,
                harvestingStart,
                harvestingEnd,
                now
        );
    }
}
