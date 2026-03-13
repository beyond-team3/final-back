package com.monsoon.seedflowplus.domain.notification.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.notification.entity.DeliveryChannel;
import com.monsoon.seedflowplus.domain.notification.entity.DeliveryStatus;
import com.monsoon.seedflowplus.domain.notification.entity.Notification;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationDelivery;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationTargetType;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationType;
import com.monsoon.seedflowplus.domain.notification.event.AccountActivatedEvent;
import com.monsoon.seedflowplus.domain.notification.event.ContractCompletedEvent;
import com.monsoon.seedflowplus.domain.notification.event.InvoiceIssuedEvent;
import com.monsoon.seedflowplus.domain.notification.event.QuotationRequestCreatedEvent;
import com.monsoon.seedflowplus.domain.notification.event.StatementIssuedEvent;
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

    public Notification createAccountActivatedNotification(AccountActivatedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return createIfNotDuplicated(
                event.userId(),
                NotificationType.ACCOUNT_ACTIVATED,
                NotificationTargetType.ACCOUNT,
                event.userId(),
                "계정이 활성화되었습니다",
                buildAccountActivatedContent(event.role()),
                event.occurredAt()
        );
    }

    public Notification createContractCompletedNotification(ContractCompletedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return createIfNotDuplicated(
                event.userId(),
                NotificationType.CONTRACT_COMPLETED,
                NotificationTargetType.CONTRACT,
                event.contractId(),
                "계약이 체결되었습니다",
                buildContractCompletedContent(event.contractCode(), event.clientName()),
                event.occurredAt()
        );
    }

    public Notification createStatementIssuedNotification(StatementIssuedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return createIfNotDuplicated(
                event.userId(),
                NotificationType.STATEMENT_ISSUED,
                NotificationTargetType.STATEMENT,
                event.statementId(),
                "명세서가 발급되었습니다",
                buildStatementIssuedContent(event.statementCode(), event.orderCode()),
                event.occurredAt()
        );
    }

    public Notification createInvoiceIssuedNotification(InvoiceIssuedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return createIfNotDuplicated(
                event.userId(),
                NotificationType.INVOICE_ISSUED,
                NotificationTargetType.INVOICE,
                event.invoiceId(),
                "청구서가 발행되었습니다",
                buildInvoiceIssuedContent(event.invoiceCode(), event.clientName()),
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

    private String buildAccountActivatedContent(Role role) {
        return switch (role) {
            case CLIENT -> "거래처 계정이 활성화되었습니다. 지금부터 서비스를 이용할 수 있습니다.";
            case SALES_REP -> "영업사원 계정이 활성화되었습니다. 지금부터 서비스를 이용할 수 있습니다.";
            case ADMIN -> "관리자 계정이 활성화되었습니다.";
        };
    }

    private String buildContractCompletedContent(String contractCode, String clientName) {
        return String.format("%s 거래처와 계약 %s가 최종 체결되었습니다.",
                clientName == null || clientName.isBlank() ? "거래처" : clientName,
                contractCode == null || contractCode.isBlank() ? "" : "(" + contractCode + ")");
    }

    private String buildStatementIssuedContent(String statementCode, String orderCode) {
        return String.format("주문 %s 기준 명세서 %s가 발급되었습니다.",
                orderCode == null || orderCode.isBlank() ? "" : "(" + orderCode + ")",
                statementCode == null || statementCode.isBlank() ? "" : "(" + statementCode + ")");
    }

    private String buildInvoiceIssuedContent(String invoiceCode, String clientName) {
        return String.format("%s 거래처 대상 청구서 %s가 발행되었습니다.",
                clientName == null || clientName.isBlank() ? "거래처" : clientName,
                invoiceCode == null || invoiceCode.isBlank() ? "" : "(" + invoiceCode + ")");
    }
}
