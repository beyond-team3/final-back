package com.monsoon.seedflowplus.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.notification.entity.Notification;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationDelivery;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationTargetType;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationType;
import com.monsoon.seedflowplus.domain.notification.event.ApprovalCompletedEvent;
import com.monsoon.seedflowplus.domain.notification.event.ApprovalRequestedEvent;
import com.monsoon.seedflowplus.domain.notification.repository.NotificationDeliveryRepository;
import com.monsoon.seedflowplus.domain.notification.repository.NotificationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DealApprovalNotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private DealApprovalNotificationService dealApprovalNotificationService;

    @Test
    @DisplayName("승인 요청 알림은 generic type을 유지하면서 실제 문서 target/title/content를 저장한다")
    void createApprovalRequestedNotificationUsesDocumentContext() {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 3, 12, 10, 0);
        ApprovalRequestedEvent event = new ApprovalRequestedEvent(
                100L,
                55L,
                DealType.QUO,
                501L,
                "QUO-20260312-001",
                ActorType.ADMIN,
                occurredAt
        );
        User user = org.mockito.Mockito.mock(User.class);
        when(entityManager.find(User.class, 100L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(user);
        when(notificationRepository.existsByUser_IdAndTypeAndTargetTypeAndTargetIdAndContentAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(100L),
                eq(NotificationType.APPROVAL_REQUESTED),
                eq(NotificationTargetType.QUOTATION),
                eq(501L),
                any(),
                any(),
                any()
        )).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", 900L);
            ReflectionTestUtils.setField(notification, "createdAt", occurredAt);
            return notification;
        });

        Notification saved = dealApprovalNotificationService.createApprovalRequestedNotification(event);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        verify(notificationDeliveryRepository).save(any(NotificationDelivery.class));

        Notification notification = notificationCaptor.getValue();
        assertThat(saved).isNotNull();
        assertThat(notification.getType()).isEqualTo(NotificationType.APPROVAL_REQUESTED);
        assertThat(notification.getTargetType()).isEqualTo(NotificationTargetType.QUOTATION);
        assertThat(notification.getTargetId()).isEqualTo(501L);
        assertThat(notification.getTitle()).isEqualTo("견적서 관리자 승인 요청");
        assertThat(notification.getContent()).contains("QUO-20260312-001");
        assertThat(notification.getContent()).contains("관리자");
    }

    @Test
    @DisplayName("동일 문서라도 승인 단계가 다르면 승인 완료 알림이 중복 제거되지 않는다")
    void createApprovalCompletedNotificationUsesStageSpecificDedupKey() {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 3, 12, 11, 0);
        ApprovalCompletedEvent event = new ApprovalCompletedEvent(
                100L,
                55L,
                DealType.CNT,
                701L,
                "CNT-20260312-001",
                ActorType.CLIENT,
                occurredAt
        );
        User user = org.mockito.Mockito.mock(User.class);
        when(entityManager.find(User.class, 100L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(user);
        when(notificationRepository.existsByUser_IdAndTypeAndTargetTypeAndTargetIdAndContentAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(100L),
                eq(NotificationType.APPROVAL_COMPLETED),
                eq(NotificationTargetType.CONTRACT),
                eq(701L),
                eq("계약서 (CNT-20260312-001) 문서가 거래처 승인되었습니다."),
                any(),
                any()
        )).thenReturn(true);

        Notification saved = dealApprovalNotificationService.createApprovalCompletedNotification(event);

        assertThat(saved).isNull();
        verify(notificationRepository, never()).save(any(Notification.class));
    }
}
