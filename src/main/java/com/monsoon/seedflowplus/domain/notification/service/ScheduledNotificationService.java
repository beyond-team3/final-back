package com.monsoon.seedflowplus.domain.notification.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduledNotificationService {

    private static final LocalTime DEFAULT_SCHEDULE_TIME = LocalTime.of(9, 0);

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final EntityManager entityManager;

    public void scheduleContractLifecycleNotifications(ContractHeader contract, List<Long> recipientUserIds) {
        Objects.requireNonNull(contract, "contract must not be null");
        if (recipientUserIds == null || recipientUserIds.isEmpty()) {
            return;
        }

        for (Long userId : recipientUserIds.stream().distinct().toList()) {
            if (userId == null) {
                continue;
            }
            if (contract.getStartDate() != null) {
                createScheduledNotification(
                        userId,
                        NotificationType.CONTRACT_STARTING,
                        NotificationTargetType.CONTRACT,
                        contract.getId(),
                        "계약 시작 알림",
                        buildContractStartingContent(contract),
                        atNineAm(contract.getStartDate())
                );
            }
            if (contract.getEndDate() != null) {
                createScheduledNotification(
                        userId,
                        NotificationType.CONTRACT_ENDING_SOON,
                        NotificationTargetType.CONTRACT,
                        contract.getId(),
                        "계약 종료 예정 알림",
                        buildContractEndingSoonContent(contract),
                        atNineAm(contract.getEndDate().minusDays(30))
                );
                createScheduledNotification(
                        userId,
                        NotificationType.CONTRACT_ENDED,
                        NotificationTargetType.CONTRACT,
                        contract.getId(),
                        "계약 종료 알림",
                        buildContractEndedContent(contract),
                        atNineAm(contract.getEndDate())
                );
            }
        }
    }

    private Notification createScheduledNotification(
            Long userId,
            NotificationType type,
            NotificationTargetType targetType,
            Long targetId,
            String title,
            String content,
            LocalDateTime scheduledAt
    ) {
        Objects.requireNonNull(scheduledAt, "scheduledAt must not be null");
        User lockedUser = lockUser(userId);
        if (isDuplicated(userId, type, targetType, targetId, scheduledAt)) {
            return null;
        }

        Notification notification = notificationRepository.save(
                Notification.builder()
                        .user(lockedUser)
                        .type(type)
                        .title(title)
                        .content(content)
                        .targetType(targetType)
                        .targetId(targetId)
                        .build()
        );

        notificationDeliveryRepository.save(NotificationDelivery.builder()
                .notification(notification)
                .channel(DeliveryChannel.IN_APP)
                .status(DeliveryStatus.PENDING)
                .scheduledAt(scheduledAt)
                .build());
        return notification;
    }

    private User lockUser(Long userId) {
        User user = entityManager.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);
        if (user == null) {
            throw new CoreException(ErrorType.USER_NOT_FOUND);
        }
        return user;
    }

    private boolean isDuplicated(
            Long userId,
            NotificationType type,
            NotificationTargetType targetType,
            Long targetId,
            LocalDateTime scheduledAt
    ) {
        return notificationDeliveryRepository.existsByNotification_UserIdAndNotification_TypeAndNotification_TargetTypeAndNotification_TargetIdAndScheduledAt(
                userId,
                type,
                targetType,
                targetId,
                scheduledAt
        );
    }

    private LocalDateTime atNineAm(LocalDate date) {
        return LocalDateTime.of(date, DEFAULT_SCHEDULE_TIME);
    }

    private String buildContractStartingContent(ContractHeader contract) {
        return String.format("계약 %s가 오늘 시작됩니다.",
                wrapCode(contract.getContractCode()));
    }

    private String buildContractEndingSoonContent(ContractHeader contract) {
        return String.format("계약 %s 종료까지 30일 남았습니다.",
                wrapCode(contract.getContractCode()));
    }

    private String buildContractEndedContent(ContractHeader contract) {
        return String.format("계약 %s가 오늘 종료됩니다.",
                wrapCode(contract.getContractCode()));
    }

    private String wrapCode(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        return "(" + code + ")";
    }
}
