package com.monsoon.seedflowplus.domain.notification.event;

import com.monsoon.seedflowplus.domain.notification.command.NotificationSseService;
import com.monsoon.seedflowplus.domain.notification.dto.response.NotificationListItemResponse;
import com.monsoon.seedflowplus.domain.notification.entity.Notification;
import com.monsoon.seedflowplus.domain.notification.service.DealApprovalNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.event.EventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventHandler {

    private final DealApprovalNotificationService dealApprovalNotificationService;
    private final NotificationSseService notificationSseService;

    @Async
    @EventListener
    @Transactional
    public void handleDealStatusChanged(DealStatusChangedEvent event) {
        Notification saved = dealApprovalNotificationService.createDealStatusChangedNotification(event);
        sendIfPresent(event.userId(), saved);
    }

    @Async
    @EventListener
    @Transactional
    public void handleApprovalRequested(ApprovalRequestedEvent event) {
        Notification saved = dealApprovalNotificationService.createApprovalRequestedNotification(event);
        sendIfPresent(event.userId(), saved);
    }

    @Async
    @EventListener
    @Transactional
    public void handleApprovalCompleted(ApprovalCompletedEvent event) {
        Notification saved = dealApprovalNotificationService.createApprovalCompletedNotification(event);
        sendIfPresent(event.userId(), saved);
    }

    @Async
    @EventListener
    @Transactional
    public void handleApprovalRejected(ApprovalRejectedEvent event) {
        Notification saved = dealApprovalNotificationService.createApprovalRejectedNotification(event);
        sendIfPresent(event.userId(), saved);
    }

    private void sendIfPresent(Long userId, Notification saved) {
        if (saved == null) {
            return;
        }
        notificationSseService.send(userId, NotificationListItemResponse.from(saved));
    }
}
