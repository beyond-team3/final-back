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
import com.monsoon.seedflowplus.domain.notification.event.QuotationRequestCreatedEvent;
import com.monsoon.seedflowplus.domain.notification.repository.NotificationDeliveryRepository;
import com.monsoon.seedflowplus.domain.notification.repository.NotificationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentNotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final EntityManager entityManager;

    public Notification createQuotationRequestCreatedNotification(QuotationRequestCreatedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return createIfNotDuplicated(
                event.userId(),
                NotificationType.QUOTATION_REQUEST_CREATED,
                NotificationTargetType.QUOTATION_REQUEST,
                event.quotationRequestId(),
                "견적요청서가 접수되었습니다",
                buildQuotationRequestCreatedContent(event.requestCode(), event.clientName()),
                event.occurredAt()
        );
    }

    private Notification createIfNotDuplicated(
            Long userId,
            NotificationType type,
            NotificationTargetType targetType,
            Long targetId,
            String title,
            String content,
            LocalDateTime now
    ) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(targetType, "targetType must not be null");
        Objects.requireNonNull(targetId, "targetId must not be null");
        Objects.requireNonNull(now, "now must not be null");

        User lockedUser = lockUser(userId);
        if (isDuplicatedToday(userId, type, targetType, targetId, now)) {
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

        NotificationDelivery immediateDelivery = NotificationDelivery.builder()
                .notification(notification)
                .channel(DeliveryChannel.IN_APP)
                .status(DeliveryStatus.PENDING)
                .scheduledAt(now)
                .build();
        immediateDelivery.markSent(now, null);
        notificationDeliveryRepository.save(immediateDelivery);

        return notification;
    }

    private User lockUser(Long userId) {
        User user = entityManager.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);
        if (user == null) {
            throw new CoreException(ErrorType.USER_NOT_FOUND);
        }
        return user;
    }

    private boolean isDuplicatedToday(
            Long userId,
            NotificationType type,
            NotificationTargetType targetType,
            Long targetId,
            LocalDateTime now
    ) {
        LocalDateTime from = now.toLocalDate().atStartOfDay();
        LocalDateTime to = from.plusDays(1);
        return notificationRepository.existsByUser_IdAndTypeAndTargetTypeAndTargetIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                userId,
                type,
                targetType,
                targetId,
                from,
                to
        );
    }

    private String buildQuotationRequestCreatedContent(String requestCode, String clientName) {
        return String.format("%s 거래처의 견적요청서 %s가 등록되었습니다.",
                clientName == null || clientName.isBlank() ? "거래처" : clientName,
                requestCode == null || requestCode.isBlank() ? "" : "(" + requestCode + ")");
    }
}
