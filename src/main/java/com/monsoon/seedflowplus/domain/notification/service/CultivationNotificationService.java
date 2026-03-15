package com.monsoon.seedflowplus.domain.notification.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.notification.entity.DeliveryChannel;
import com.monsoon.seedflowplus.domain.notification.entity.DeliveryStatus;
import com.monsoon.seedflowplus.domain.notification.entity.Notification;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationDelivery;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationTargetType;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationType;
import com.monsoon.seedflowplus.domain.notification.repository.NotificationDeliveryRepository;
import com.monsoon.seedflowplus.domain.notification.repository.NotificationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CultivationNotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final EntityManager entityManager;

    @Transactional
    public void createSowingPromotionNotification(
            Long userId,
            Long productId,
            String productName,
            Integer sowingStartMonth,
            Integer clientCount,
            LocalDateTime scheduledAt
    ) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(productName, "productName must not be null");
        Objects.requireNonNull(sowingStartMonth, "sowingStartMonth must not be null");
        Objects.requireNonNull(clientCount, "clientCount must not be null");
        Objects.requireNonNull(scheduledAt, "scheduledAt must not be null");

        User lockedUser = lockUser(userId);
        NotificationType type = NotificationType.CULTIVATION_SOWING_PROMOTION;
        if (isDuplicatedByScheduledAt(userId, type, NotificationTargetType.PRODUCT, productId, scheduledAt)) {
            return;
        }

        Notification notification = Notification.builder()
                .user(lockedUser)
                .type(type)
                .title(buildSowingPromotionTitle(productName))
                .content(buildSowingPromotionContent(productName, sowingStartMonth, clientCount))
                .targetType(NotificationTargetType.PRODUCT)
                .targetId(productId)
                .build();
        Notification savedNotification = notificationRepository.save(notification);

        NotificationDelivery delivery = NotificationDelivery.builder()
                .notification(savedNotification)
                .channel(DeliveryChannel.IN_APP)
                .status(DeliveryStatus.PENDING)
                .scheduledAt(scheduledAt)
                .build();
        notificationDeliveryRepository.save(delivery);
    }

    @Transactional
    public void createHarvestFeedbackNotification(
            Long userId,
            Long productId,
            String productName,
            Integer harvestingStartMonth,
            Integer clientCount,
            LocalDateTime scheduledAt
    ) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(productName, "productName must not be null");
        Objects.requireNonNull(harvestingStartMonth, "harvestingStartMonth must not be null");
        Objects.requireNonNull(clientCount, "clientCount must not be null");
        Objects.requireNonNull(scheduledAt, "scheduledAt must not be null");

        User lockedUser = lockUser(userId);
        NotificationType type = NotificationType.CULTIVATION_HARVEST_FEEDBACK;
        if (isDuplicatedByScheduledAt(userId, type, NotificationTargetType.PRODUCT, productId, scheduledAt)) {
            return;
        }

        Notification notification = Notification.builder()
                .user(lockedUser)
                .type(type)
                .title(buildHarvestFeedbackTitle(productName))
                .content(buildHarvestFeedbackContent(productName, harvestingStartMonth, clientCount))
                .targetType(NotificationTargetType.PRODUCT)
                .targetId(productId)
                .build();
        Notification savedNotification = notificationRepository.save(notification);

        NotificationDelivery delivery = NotificationDelivery.builder()
                .notification(savedNotification)
                .channel(DeliveryChannel.IN_APP)
                .status(DeliveryStatus.PENDING)
                .scheduledAt(scheduledAt)
                .build();
        notificationDeliveryRepository.save(delivery);
    }

    private User lockUser(Long userId) {
        User user = entityManager.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);
        if (user == null) {
            throw new CoreException(ErrorType.USER_NOT_FOUND);
        }
        return user;
    }

    private boolean isDuplicatedByScheduledAt(
            Long userId,
            NotificationType type,
            NotificationTargetType targetType,
            Long targetId,
            LocalDateTime scheduledAt
    ) {
        return notificationDeliveryRepository
                .existsByNotification_UserIdAndNotification_TypeAndNotification_TargetTypeAndNotification_TargetIdAndScheduledAt(
                userId,
                type,
                targetType,
                targetId,
                scheduledAt);
    }

    private String buildSowingPromotionTitle(String productName) {
        return String.format("%s 파종 준비 시기입니다", productName);
    }

    private String buildSowingPromotionContent(String productName, Integer sowingStartMonth, Integer clientCount) {
        return String.format(
                Locale.KOREAN,
                "담당 거래처 %d곳에서 취급하는 %s의 파종 권장 시작 월은 %d월입니다. 지금 판매 전략과 품목 구성을 점검해 보세요.",
                clientCount,
                productName,
                sowingStartMonth
        );
    }

    private String buildHarvestFeedbackTitle(String productName) {
        return String.format("담당 거래처의 %s 수확기가 시작됩니다", productName);
    }

    private String buildHarvestFeedbackContent(String productName, Integer harvestingStartMonth, Integer clientCount) {
        return String.format(
                Locale.KOREAN,
                "담당 거래처 %d곳에서 취급하는 %s의 수확 시작 월은 %d월입니다. 고객 피드백을 수집해 다음 재배 계획에 반영해 보세요.",
                clientCount,
                productName,
                harvestingStartMonth
        );
    }
}
