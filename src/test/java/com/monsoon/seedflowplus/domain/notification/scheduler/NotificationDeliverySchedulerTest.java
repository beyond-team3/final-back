package com.monsoon.seedflowplus.domain.notification.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.notification.command.NotificationCommandService;
import com.monsoon.seedflowplus.domain.notification.command.NotificationDeliveryWorkerService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NotificationDeliverySchedulerTest {

    @Mock
    private NotificationDeliveryWorkerService notificationDeliveryWorkerService;

    @Mock
    private NotificationCommandService notificationCommandService;

    @Mock
    private Clock clock;

    @InjectMocks
    private NotificationDeliveryScheduler notificationDeliveryScheduler;

    @Test
    @DisplayName("dispatchDueDeliveries는 현재 시각으로 워커를 호출한다")
    void dispatchDueDeliveries() {
        Instant nowInstant = Instant.parse("2026-03-06T08:00:00Z");
        when(clock.instant()).thenReturn(nowInstant);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        notificationDeliveryScheduler.dispatchDueDeliveries();

        verify(notificationDeliveryWorkerService)
                .dispatchDueDeliveries(LocalDateTime.ofInstant(nowInstant, ZoneId.of("UTC")));
    }

    @Test
    @DisplayName("deleteExpiredNotifications는 30일 이전 cutoff로 삭제를 호출한다")
    void deleteExpiredNotifications() {
        Instant nowInstant = Instant.parse("2026-03-06T08:00:00Z");
        when(clock.instant()).thenReturn(nowInstant);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        ReflectionTestUtils.setField(notificationDeliveryScheduler, "retentionDays", 30L);

        notificationDeliveryScheduler.deleteExpiredNotifications();

        verify(notificationCommandService)
                .deleteOlderThan(LocalDateTime.ofInstant(nowInstant, ZoneId.of("UTC")).minusDays(30));
    }
}
