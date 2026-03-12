package com.monsoon.seedflowplus.domain.notification.event;

import com.monsoon.seedflowplus.domain.notification.command.NotificationSseService;
import com.monsoon.seedflowplus.domain.notification.dto.response.NotificationListItemResponse;
import com.monsoon.seedflowplus.domain.notification.entity.Notification;
import com.monsoon.seedflowplus.domain.notification.service.DocumentNotificationService;
import com.monsoon.seedflowplus.domain.notification.service.DealApprovalNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class NotificationEventHandler {

    private final DealApprovalNotificationService dealApprovalNotificationService;
    private final DocumentNotificationService documentNotificationService;
    private final NotificationSseService notificationSseService;

    @Async("notificationTaskExecutor")
    @EventListener
    @Transactional
    public void handleDealStatusChanged(DealStatusChangedEvent event) {
        Notification saved = dealApprovalNotificationService.createDealStatusChangedNotification(event);
        sendIfPresent(event.userId(), saved);
    }

    @Async("notificationTaskExecutor")
    @EventListener
    @Transactional
    public void handleApprovalRequested(ApprovalRequestedEvent event) {
        Notification saved = dealApprovalNotificationService.createApprovalRequestedNotification(event);
        sendIfPresent(event.userId(), saved);
    }

    @Async("notificationTaskExecutor")
    @EventListener
    @Transactional
    public void handleApprovalCompleted(ApprovalCompletedEvent event) {
        Notification saved = dealApprovalNotificationService.createApprovalCompletedNotification(event);
        sendIfPresent(event.userId(), saved);
    }

    @Async("notificationTaskExecutor")
    @EventListener
    @Transactional
    public void handleApprovalRejected(ApprovalRejectedEvent event) {
        Notification saved = dealApprovalNotificationService.createApprovalRejectedNotification(event);
        sendIfPresent(event.userId(), saved);
    }

    @Async("notificationTaskExecutor")
    @EventListener
    @Transactional
    public void handleQuotationRequestCreated(QuotationRequestCreatedEvent event) {
        Notification saved = documentNotificationService.createQuotationRequestCreatedNotification(event);
        sendIfPresent(event.userId(), saved);
    }

    @Async("notificationTaskExecutor")
    @EventListener
    @Transactional
    public void handleContractCompleted(ContractCompletedEvent event) {
        Notification saved = documentNotificationService.createContractCompletedNotification(event);
        sendIfPresent(event.userId(), saved);
    }

    @Async("notificationTaskExecutor")
    @EventListener
    @Transactional
    public void handleStatementIssued(StatementIssuedEvent event) {
        Notification saved = documentNotificationService.createStatementIssuedNotification(event);
        sendIfPresent(event.userId(), saved);
    }

    @Async("notificationTaskExecutor")
    @EventListener
    @Transactional
    public void handleInvoiceIssued(InvoiceIssuedEvent event) {
        Notification saved = documentNotificationService.createInvoiceIssuedNotification(event);
        sendIfPresent(event.userId(), saved);
    }

    private void sendIfPresent(Long userId, Notification saved) {
        if (saved == null) {
            return;
        }
        NotificationListItemResponse payload = NotificationListItemResponse.from(saved);
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            notificationSseService.send(userId, payload);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationSseService.send(userId, payload);
            }
        });
    }
}
