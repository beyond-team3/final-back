package com.monsoon.seedflowplus.domain.notification.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.notification.command.NotificationSseService;
import com.monsoon.seedflowplus.domain.notification.entity.Notification;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationTargetType;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationType;
import com.monsoon.seedflowplus.domain.notification.service.DealApprovalNotificationService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationEventHandlerTest {

    @Mock
    private DealApprovalNotificationService dealApprovalNotificationService;

    @Mock
    private NotificationSseService notificationSseService;

    @InjectMocks
    private NotificationEventHandler notificationEventHandler;

    @Test
    @DisplayName("ApprovalRequested 이벤트 수신 시 생성 서비스 위임 후 SSE 전송한다")
    void handleApprovalRequested() {
        ApprovalRequestedEvent event = new ApprovalRequestedEvent(100L, 11L, DealType.QUO, 501L, LocalDateTime.now());
        Notification saved = notification(1L);

        when(dealApprovalNotificationService.createApprovalRequestedNotification(event)).thenReturn(saved);

        notificationEventHandler.handleApprovalRequested(event);

        verify(dealApprovalNotificationService).createApprovalRequestedNotification(event);
        verify(notificationSseService).send(eq(100L), any());
    }

    @Test
    @DisplayName("중복으로 알림이 생성되지 않으면 SSE는 전송하지 않는다")
    void handleApprovalRequestedNoSendWhenDuplicated() {
        ApprovalRequestedEvent event = new ApprovalRequestedEvent(100L, 11L, DealType.QUO, 501L, LocalDateTime.now());
        when(dealApprovalNotificationService.createApprovalRequestedNotification(event)).thenReturn(null);

        notificationEventHandler.handleApprovalRequested(event);

        verify(dealApprovalNotificationService).createApprovalRequestedNotification(event);
        verify(notificationSseService, never()).send(any(), any());
    }

    private Notification notification(Long id) {
        Notification notification = Notification.builder()
                .user(org.mockito.Mockito.mock(com.monsoon.seedflowplus.domain.account.entity.User.class))
                .type(NotificationType.APPROVAL_REQUESTED)
                .title("title")
                .content("content")
                .targetType(NotificationTargetType.APPROVAL)
                .targetId(11L)
                .build();
        ReflectionTestUtils.setField(notification, "id", id);
        ReflectionTestUtils.setField(notification, "createdAt", LocalDateTime.now());
        return notification;
    }
}
