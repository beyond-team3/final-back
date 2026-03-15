package com.monsoon.seedflowplus.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.notification.entity.Notification;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationDelivery;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationTargetType;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationType;
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
class CultivationNotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private CultivationNotificationService cultivationNotificationService;

    @Test
    @DisplayName("파종 프로모션 알림은 scheduledAt 기준 중복이 없으면 상품명 문구로 생성한다")
    void createSowingPromotionNotification() {
        User user = createUser(100L);
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 3, 1, 9, 0);
        when(entityManager.find(User.class, 100L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(user);
        when(notificationDeliveryRepository
                .existsByNotification_UserIdAndNotification_TypeAndNotification_TargetTypeAndNotification_TargetIdAndScheduledAt(
                        100L,
                        NotificationType.CULTIVATION_SOWING_PROMOTION,
                        NotificationTargetType.PRODUCT,
                        200L,
                        scheduledAt))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            ReflectionTestUtils.setField(notification, "id", 1L);
            return notification;
        });

        cultivationNotificationService.createSowingPromotionNotification(
                100L,
                200L,
                "수박",
                4,
                2,
                scheduledAt);

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        ArgumentCaptor<NotificationDelivery> deliveryCaptor = ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        verify(notificationDeliveryRepository).save(deliveryCaptor.capture());

        assertThat(notificationCaptor.getValue().getTitle()).isEqualTo("수박 파종 준비 시기입니다");
        assertThat(notificationCaptor.getValue().getContent()).contains("담당 거래처 2곳", "수박", "4월");
        assertThat(deliveryCaptor.getValue().getScheduledAt()).isEqualTo(scheduledAt);
    }

    @Test
    @DisplayName("수확 피드백 알림은 같은 예약 시각 delivery가 있으면 중복 생성하지 않는다")
    void createHarvestFeedbackNotificationDedupesByScheduledAt() {
        User user = createUser(100L);
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 7, 1, 9, 0);
        when(entityManager.find(User.class, 100L, LockModeType.PESSIMISTIC_WRITE)).thenReturn(user);
        when(notificationDeliveryRepository
                .existsByNotification_UserIdAndNotification_TypeAndNotification_TargetTypeAndNotification_TargetIdAndScheduledAt(
                        100L,
                        NotificationType.CULTIVATION_HARVEST_FEEDBACK,
                        NotificationTargetType.PRODUCT,
                        200L,
                        scheduledAt))
                .thenReturn(true);

        cultivationNotificationService.createHarvestFeedbackNotification(
                100L,
                200L,
                "수박",
                7,
                1,
                scheduledAt);

        verify(notificationRepository, never()).save(any(Notification.class));
        verify(notificationDeliveryRepository, never()).save(any(NotificationDelivery.class));
    }

    private User createUser(Long userId) {
        User user = User.builder()
                .loginId("user-" + userId)
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.SALES_REP)
                .employee(null)
                .client(null)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
