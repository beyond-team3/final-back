package com.monsoon.seedflowplus.domain.notification.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
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
        String content = buildApprovalRequestedContent(event.dealType(), event.targetCode(), event.actorType());
        return createIfNotDuplicated(
                event.userId(),
                NotificationType.APPROVAL_REQUESTED,
                resolveTargetType(event.dealType()),
                event.targetId(),
                buildApprovalRequestedTitle(event.dealType(), event.actorType()),
                content,
                content,
                event.occurredAt()
        );
    }

    public Notification createApprovalCompletedNotification(ApprovalCompletedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        String content = buildApprovalCompletedContent(event.dealType(), event.targetCode(), event.actorType());
        return createIfNotDuplicated(
                event.userId(),
                NotificationType.APPROVAL_COMPLETED,
                resolveTargetType(event.dealType()),
                event.targetId(),
                buildApprovalCompletedTitle(event.dealType(), event.actorType()),
                content,
                content,
                event.occurredAt()
        );
    }

    public Notification createApprovalRejectedNotification(ApprovalRejectedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        String content = buildApprovalRejectedContent(event.dealType(), event.targetCode(), event.actorType());
        return createIfNotDuplicated(
                event.userId(),
                NotificationType.APPROVAL_REJECTED,
                resolveTargetType(event.dealType()),
                event.targetId(),
                buildApprovalRejectedTitle(event.dealType(), event.actorType()),
                content,
                content,
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

    private String buildApprovalRequestedTitle(DealType dealType, ActorType actorType) {
        return String.format("%s %s 승인 요청", toDealLabel(dealType), toActorLabel(actorType));
    }

    private String buildApprovalRequestedContent(DealType dealType, String targetCode, ActorType actorType) {
        return String.format("%s %s 문서 %s의 승인이 필요합니다.",
                toDealLabel(dealType),
                formatDocumentCode(targetCode),
                toActorActionLabel(actorType));
    }

    private String buildApprovalCompletedTitle(DealType dealType, ActorType actorType) {
        return String.format("%s %s 승인 완료", toDealLabel(dealType), toActorLabel(actorType));
    }

    private String buildApprovalCompletedContent(DealType dealType, String targetCode, ActorType actorType) {
        return String.format("%s %s 문서가 %s 승인되었습니다.",
                toDealLabel(dealType),
                formatDocumentCode(targetCode),
                toActorLabel(actorType));
    }

    private String buildApprovalRejectedTitle(DealType dealType, ActorType actorType) {
        return String.format("%s %s 승인 반려", toDealLabel(dealType), toActorLabel(actorType));
    }

    private String buildApprovalRejectedContent(DealType dealType, String targetCode, ActorType actorType) {
        return String.format("%s %s 문서가 %s 반려되었습니다.",
                toDealLabel(dealType),
                formatDocumentCode(targetCode),
                toActorLabel(actorType));
    }

    private NotificationTargetType resolveTargetType(DealType dealType) {
        return switch (dealType) {
            case QUO -> NotificationTargetType.QUOTATION;
            case CNT -> NotificationTargetType.CONTRACT;
            case ORD -> NotificationTargetType.ORDER;
            default -> NotificationTargetType.APPROVAL;
        };
    }

    private String toDealLabel(DealType dealType) {
        return switch (dealType) {
            case QUO -> "견적서";
            case CNT -> "계약서";
            case ORD -> "주문서";
            default -> dealType.name();
        };
    }

    private String toActorLabel(ActorType actorType) {
        return switch (actorType) {
            case ADMIN -> "관리자";
            case CLIENT -> "거래처";
            case SALES_REP -> "영업사원";
            case SYSTEM -> "시스템";
        };
    }

    private String toActorActionLabel(ActorType actorType) {
        return switch (actorType) {
            case ADMIN -> "관리자";
            case CLIENT -> "거래처";
            case SALES_REP -> "영업사원";
            case SYSTEM -> "시스템";
        };
    }

    private String formatDocumentCode(String targetCode) {
        if (targetCode == null || targetCode.isBlank()) {
            return "";
        }
        return "(" + targetCode + ")";
    }

    private String nullToDash(String value) {
        return value == null ? "-" : value;
    }
}
