package com.monsoon.seedflowplus.domain.notification.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.notification.entity.Notification;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationTargetType;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationType;
import com.monsoon.seedflowplus.domain.notification.repository.NotificationDeliveryRepository;
import com.monsoon.seedflowplus.domain.notification.repository.NotificationRepository;
import com.monsoon.seedflowplus.domain.notification.service.CultivationNotificationService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationCommandServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Mock
    private CultivationNotificationService cultivationNotificationService;

    @InjectMocks
    private NotificationCommandService notificationCommandService;

    @Test
    @DisplayName("단건 삭제는 소유 검증 후 delivery와 notification을 함께 삭제한다")
    void deleteOne() {
        Notification notification = notification(10L);
        when(notificationRepository.findByIdAndUser_Id(10L, 100L)).thenReturn(Optional.of(notification));

        notificationCommandService.deleteOne(100L, 10L);

        verify(notificationDeliveryRepository).deleteByNotification_Id(10L);
        verify(notificationRepository).delete(notification);
    }

    @Test
    @DisplayName("단건 삭제 시 소유 알림이 없으면 NOTIFICATION_NOT_FOUND")
    void deleteOneNotFound() {
        when(notificationRepository.findByIdAndUser_Id(10L, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationCommandService.deleteOne(100L, 10L))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.NOTIFICATION_NOT_FOUND);

        verify(notificationDeliveryRepository, never()).deleteByNotification_Id(10L);
        verify(notificationRepository, never()).delete(org.mockito.ArgumentMatchers.any(Notification.class));
    }

    @Test
    @DisplayName("전체 삭제는 사용자 기준 delivery와 notification을 벌크 삭제한다")
    void deleteAll() {
        notificationCommandService.deleteAll(100L);

        verify(notificationDeliveryRepository).deleteByNotification_User_Id(100L);
        verify(notificationRepository).deleteByUser_Id(100L);
    }

    private Notification notification(Long id) {
        Notification notification = Notification.builder()
                .user(org.mockito.Mockito.mock(com.monsoon.seedflowplus.domain.account.entity.User.class))
                .type(NotificationType.CULTIVATION_SOWING_PROMOTION)
                .title("title")
                .content("content")
                .targetType(NotificationTargetType.PRODUCT)
                .targetId(1L)
                .build();
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }
}
