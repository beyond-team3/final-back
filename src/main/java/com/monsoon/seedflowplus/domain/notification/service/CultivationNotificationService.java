package com.monsoon.seedflowplus.domain.notification.service;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CultivationNotificationService {

    private static final LocalTime DEFAULT_SCHEDULE_TIME = LocalTime.of(9, 0);

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final EntityManager entityManager;

    public void createSowingPromotionNotification(
            Long userId,
            Long productId,
            LocalDateTime sowingStart,
            LocalDateTime sowingEnd,
            LocalDateTime now
    ) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(sowingStart, "sowingStart must not be null");
        Objects.requireNonNull(sowingEnd, "sowingEnd must not be null");
        Objects.requireNonNull(now, "now must not be null");

        NotificationType type = NotificationType.CULTIVATION_SOWING_PROMOTION;
        if (isDuplicatedToday(userId, type, NotificationTargetType.PRODUCT, productId, now)) {
            return;
        }

        Notification notification = Notification.builder()
                .user(entityManager.getReference(User.class, userId))
                .type(type)
                .title(buildSowingPromotionTitle())
                .content(buildSowingPromotionContent(sowingStart, sowingEnd))
                .targetType(NotificationTargetType.PRODUCT)
                .targetId(productId)
                .build();
        Notification savedNotification = notificationRepository.save(notification);

        NotificationDelivery delivery = NotificationDelivery.builder()
                .notification(savedNotification)
                .channel(DeliveryChannel.IN_APP)
                .status(DeliveryStatus.PENDING)
                .scheduledAt(calcSowingScheduledAt(sowingStart))
                .build();
        notificationDeliveryRepository.save(delivery);
    }

    public void createHarvestFeedbackNotification(
            Long userId,
            Long productId,
            LocalDateTime harvestingStart,
            LocalDateTime harvestingEnd,
            LocalDateTime now
    ) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(harvestingStart, "harvestingStart must not be null");
        Objects.requireNonNull(harvestingEnd, "harvestingEnd must not be null");
        Objects.requireNonNull(now, "now must not be null");

        NotificationType type = NotificationType.CULTIVATION_HARVEST_FEEDBACK;
        if (isDuplicatedToday(userId, type, NotificationTargetType.PRODUCT, productId, now)) {
            return;
        }

        Notification notification = Notification.builder()
                .user(entityManager.getReference(User.class, userId))
                .type(type)
                .title(buildHarvestFeedbackTitle())
                .content(buildHarvestFeedbackContent(harvestingStart, harvestingEnd))
                .targetType(NotificationTargetType.PRODUCT)
                .targetId(productId)
                .build();
        Notification savedNotification = notificationRepository.save(notification);

        NotificationDelivery delivery = NotificationDelivery.builder()
                .notification(savedNotification)
                .channel(DeliveryChannel.IN_APP)
                .status(DeliveryStatus.PENDING)
                .scheduledAt(calcHarvestScheduledAt(harvestingStart))
                .build();
        notificationDeliveryRepository.save(delivery);
    }

    private boolean isDuplicatedToday(
            Long userId,
            NotificationType type,
            NotificationTargetType targetType,
            Long targetId,
            LocalDateTime now
    ) {
        LocalDateTime from = calcDedupFrom(now);
        LocalDateTime to = calcDedupTo(now);

        return notificationRepository.existsByUser_IdAndTypeAndTargetTypeAndTargetIdAndCreatedAtBetween(
                userId,
                type,
                targetType,
                targetId,
                from,
                to
        );
    }

    private LocalDateTime calcDedupFrom(LocalDateTime now) {
        return now.toLocalDate().atStartOfDay();
    }

    private LocalDateTime calcDedupTo(LocalDateTime now) {
        return now.toLocalDate().plusDays(1).atStartOfDay();
    }

    private LocalDateTime calcSowingScheduledAt(LocalDateTime sowingStart) {
        LocalDate targetDate = sowingStart.toLocalDate().minusMonths(1);
        return LocalDateTime.of(targetDate, DEFAULT_SCHEDULE_TIME);
    }

    private LocalDateTime calcHarvestScheduledAt(LocalDateTime harvestingStart) {
        return LocalDateTime.of(harvestingStart.toLocalDate(), DEFAULT_SCHEDULE_TIME);
    }

    private String buildSowingPromotionTitle() {
        // TODO: template/message source 분리
        return "재배적기 알림: 파종 준비를 시작하세요";
    }

    private String buildSowingPromotionContent(LocalDateTime sowingStart, LocalDateTime sowingEnd) {
        // TODO: template/message source 분리
        return String.format(
                "파종 권장 기간은 %s ~ %s 입니다. 지금 판매 전략과 품목 구성을 점검해 보세요.",
                sowingStart.toLocalDate(),
                sowingEnd.toLocalDate()
        );
    }

    private String buildHarvestFeedbackTitle() {
        // TODO: template/message source 분리
        return "재배적기 알림: 수확기 피드백을 수집해 주세요";
    }

    private String buildHarvestFeedbackContent(LocalDateTime harvestingStart, LocalDateTime harvestingEnd) {
        // TODO: template/message source 분리
        return String.format(
                "수확 권장 기간은 %s ~ %s 입니다. 고객 피드백을 수집해 다음 재배 계획에 반영해 보세요.",
                harvestingStart.toLocalDate(),
                harvestingEnd.toLocalDate()
        );
    }
}
