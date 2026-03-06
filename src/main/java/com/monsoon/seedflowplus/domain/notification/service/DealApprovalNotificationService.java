package com.monsoon.seedflowplus.domain.notification.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.notification.entity.DeliveryChannel;
import com.monsoon.seedflowplus.domain.notification.entity.DeliveryStatus;
import com.monsoon.seedflowplus.domain.notification.entity.Notification;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationDelivery;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationTargetType;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationType;
import com.monsoon.seedflowplus.domain.notification.event.ApprovalCompletedEvent;
import com.monsoon.seedflowplus.domain.notification.event.ApprovalRejectedEvent;
import com.monsoon.seedflowplus.domain.notification.event.ApprovalRequestedEvent;
import com.monsoon.seedflowplus.domain.notification.event.DealStatusChangedEvent;
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
public class DealApprovalNotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final EntityManager entityManager;

    public Notification createDealStatusChangedNotification(DealStatusChangedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        String transitionContent = buildDealStatusChangedContent(event.fromStatus(), event.toStatus());
        return createIfNotDuplicated(
                event.userId(),
                NotificationType.DEAL_STATUS_CHANGED,
                NotificationTargetType.DEAL,
                event.dealId(),
                "딜 상태가 변경되었습니다",
                transitionContent,
                transitionContent,
                event.occurredAt()
        );
    }

    public Notification createApprovalRequestedNotification(ApprovalRequestedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return createIfNotDuplicated(
                event.userId(),
                NotificationType.APPROVAL_REQUESTED,
                NotificationTargetType.APPROVAL,
                event.approvalRequestId(),
                "승인 요청이 도착했습니다",
                buildApprovalRequestedContent(event.dealType(), event.targetId()),
                null,
                event.occurredAt()
        );
    }

    public Notification createApprovalCompletedNotification(ApprovalCompletedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return createIfNotDuplicated(
                event.userId(),
                NotificationType.APPROVAL_COMPLETED,
                NotificationTargetType.APPROVAL,
                event.approvalRequestId(),
                "승인이 완료되었습니다",
                buildApprovalCompletedContent(event.dealType(), event.targetId()),
                null,
                event.occurredAt()
        );
    }

    public Notification createApprovalRejectedNotification(ApprovalRejectedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return createIfNotDuplicated(
                event.userId(),
                NotificationType.APPROVAL_REJECTED,
                NotificationTargetType.APPROVAL,
                event.approvalRequestId(),
                "승인이 반려되었습니다",
                buildApprovalRejectedContent(event.dealType(), event.targetId()),
                null,
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
            String dedupKey,
            LocalDateTime now
    ) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(targetType, "targetType must not be null");
        Objects.requireNonNull(targetId, "targetId must not be null");
        Objects.requireNonNull(now, "now must not be null");

        User lockedUser = lockUser(userId);
        if (isDuplicatedToday(userId, type, targetType, targetId, dedupKey, now)) {
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
            String dedupKey,
            LocalDateTime now
    ) {
        LocalDateTime from = now.toLocalDate().atStartOfDay();
        LocalDateTime to = from.plusDays(1);
        if (dedupKey == null) {
            return notificationRepository.existsByUser_IdAndTypeAndTargetTypeAndTargetIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                    userId,
                    type,
                    targetType,
                    targetId,
                    from,
                    to
            );
        }
        return notificationRepository.existsByUser_IdAndTypeAndTargetTypeAndTargetIdAndContentAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                userId,
                type,
                targetType,
                targetId,
                dedupKey,
                from,
                to
        );
    }

    private String buildDealStatusChangedContent(String fromStatus, String toStatus) {
        return String.format("딜 상태가 %s에서 %s로 변경되었습니다.", nullToDash(fromStatus), nullToDash(toStatus));
    }

    private String buildApprovalRequestedContent(DealType dealType, Long targetId) {
        return String.format("%s 문서(%d)에 대한 승인 요청이 등록되었습니다.", dealType.name(), targetId);
    }

    private String buildApprovalCompletedContent(DealType dealType, Long targetId) {
        return String.format("%s 문서(%d) 승인 절차가 완료되었습니다.", dealType.name(), targetId);
    }

    private String buildApprovalRejectedContent(DealType dealType, Long targetId) {
        return String.format("%s 문서(%d) 승인 요청이 반려되었습니다.", dealType.name(), targetId);
    }

    private String nullToDash(String value) {
        return value == null ? "-" : value;
    }
}
