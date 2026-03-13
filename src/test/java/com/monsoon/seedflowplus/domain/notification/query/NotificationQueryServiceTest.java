package com.monsoon.seedflowplus.domain.notification.query;

import static org.mockito.Mockito.verify;

import com.monsoon.seedflowplus.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationQueryService notificationQueryService;

    @Test
    @DisplayName("알림 목록 조회는 발송 완료된 알림만 조회하는 저장소 메서드를 사용한다")
    void getMyNotificationsUsesVisibleQuery() {
        notificationQueryService.getMyNotifications(100L, PageRequest.of(0, 20));

        verify(notificationRepository).findVisibleByUserIdOrderByCreatedAtDesc(100L, PageRequest.of(0, 20));
    }

    @Test
    @DisplayName("미읽음 수 조회는 발송 완료된 알림만 집계한다")
    void getUnreadCountUsesVisibleQuery() {
        notificationQueryService.getUnreadCount(100L);

        verify(notificationRepository).countVisibleUnreadByUserId(100L);
    }
}
