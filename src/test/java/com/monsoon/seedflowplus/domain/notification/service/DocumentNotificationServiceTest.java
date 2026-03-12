package com.monsoon.seedflowplus.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.account.entity.User;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DocumentNotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private DocumentNotificationService documentNotificationService;

    @Test
    @DisplayName("견적요청서 생성 알림은 담당 영업사원 1명 대상 문서 알림으로 저장된다")
    void createQuotationRequestCreatedNotification() {
        LocalDateTime occurredAt = LocalDateTime.of(2026, 3, 12, 13, 0);
        QuotationRequestCreatedEvent event = new QuotationRequestCreatedEvent(
                100L,
                31L,
                "RFQ-20260312-31",
                "새봄농산",
                occurredAt
        );
        User user = org.mockito.Mockito.mock(User.class);
        when(entityManager.find(User.class, 100L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(user);
        when(notificationRepository.existsByUser_IdAndTypeAndTargetTypeAndTargetIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(100L),
                eq(NotificationType.QUOTATION_REQUEST_CREATED),
                eq(NotificationTargetType.QUOTATION_REQUEST),
                eq(31L),
                any(),
                any()
        )).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", 801L);
            ReflectionTestUtils.setField(notification, "createdAt", occurredAt);
            return notification;
        });

        Notification saved = documentNotificationService.createQuotationRequestCreatedNotification(event);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        verify(notificationDeliveryRepository).save(any(NotificationDelivery.class));

        Notification notification = notificationCaptor.getValue();
        assertThat(saved).isNotNull();
        assertThat(notification.getType()).isEqualTo(NotificationType.QUOTATION_REQUEST_CREATED);
        assertThat(notification.getTargetType()).isEqualTo(NotificationTargetType.QUOTATION_REQUEST);
        assertThat(notification.getTargetId()).isEqualTo(31L);
        assertThat(notification.getTitle()).isEqualTo("견적요청서가 접수되었습니다");
        assertThat(notification.getContent()).contains("새봄농산");
        assertThat(notification.getContent()).contains("RFQ-20260312-31");
    }
}
