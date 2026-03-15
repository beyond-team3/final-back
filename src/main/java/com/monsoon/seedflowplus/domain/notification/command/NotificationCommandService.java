package com.monsoon.seedflowplus.domain.notification.command;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.notification.repository.NotificationDeliveryRepository;
import com.monsoon.seedflowplus.domain.notification.repository.NotificationRepository;
import com.monsoon.seedflowplus.domain.notification.service.CultivationNotificationService;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final CultivationNotificationService cultivationNotificationService;

    @Transactional
    public void markAsRead(Long userId, Long notificationId, LocalDateTime now) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(notificationId, "notificationId must not be null");
        Objects.requireNonNull(now, "now must not be null");

        var notification = notificationRepository.findVisibleByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOTIFICATION_NOT_FOUND));
        notification.markAsRead(now);
    }

    @Transactional
    public void markAllAsRead(Long userId, LocalDateTime now) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(now, "now must not be null");

        notificationRepository.markAllVisibleAsRead(userId, now);
    }

    @Transactional
    public void deleteOne(Long userId, Long notificationId) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(notificationId, "notificationId must not be null");

        var notification = notificationRepository.findVisibleByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOTIFICATION_NOT_FOUND));

        notificationDeliveryRepository.deleteByNotification_Id(notification.getId());
        notificationRepository.delete(notification);
    }

    @Transactional
    public void deleteAll(Long userId) {
        Objects.requireNonNull(userId, "userId must not be null");

        notificationDeliveryRepository.deleteVisibleByNotification_User_Id(userId);
        notificationRepository.deleteVisibleByUser_Id(userId);
    }

    @Transactional
    public long deleteOlderThan(LocalDateTime cutoff) {
        Objects.requireNonNull(cutoff, "cutoff must not be null");

        notificationDeliveryRepository.deleteByNotification_CreatedAtBefore(cutoff);
        return notificationRepository.deleteByCreatedAtBefore(cutoff);
    }

    @Transactional
    public void createCultivationSowingPromotion(
            Long userId,
            Long productId,
            String productName,
            Integer sowingStartMonth,
            Integer clientCount,
            LocalDateTime scheduledAt
    ) {
        cultivationNotificationService.createSowingPromotionNotification(
                userId,
                productId,
                productName,
                sowingStartMonth,
                clientCount,
                scheduledAt
        );
    }

    @Transactional
    public void createCultivationHarvestFeedback(
            Long userId,
            Long productId,
            String productName,
            Integer harvestingStartMonth,
            Integer clientCount,
            LocalDateTime scheduledAt
    ) {
        cultivationNotificationService.createHarvestFeedbackNotification(
                userId,
                productId,
                productName,
                harvestingStartMonth,
                clientCount,
                scheduledAt
        );
    }
}
