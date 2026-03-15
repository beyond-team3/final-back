package com.monsoon.seedflowplus.domain.notification.scheduler;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.notification.command.NotificationCommandService;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationType;
import com.monsoon.seedflowplus.domain.product.service.CultivationNotificationCandidate;
import com.monsoon.seedflowplus.domain.product.service.ProductCultivationAlertService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CultivationNotificationSchedulerTest {

    @Mock
    private ProductCultivationAlertService productCultivationAlertService;

    @Mock
    private NotificationCommandService notificationCommandService;

    @Mock
    private Clock clock;

    @InjectMocks
    private CultivationNotificationScheduler cultivationNotificationScheduler;

    @Test
    @DisplayName("재배 알림 스케줄러는 후보 타입별로 command service를 호출한다")
    void createCultivationNotifications() {
        Instant nowInstant = Instant.parse("2026-03-15T01:00:00Z");
        LocalDateTime now = LocalDateTime.ofInstant(nowInstant, ZoneId.of("UTC"));
        when(clock.instant()).thenReturn(nowInstant);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(productCultivationAlertService.getNotificationCandidates(now)).thenReturn(List.of(
                CultivationNotificationCandidate.builder()
                        .type(NotificationType.CULTIVATION_SOWING_PROMOTION)
                        .userId(1L)
                        .productId(10L)
                        .productName("수박")
                        .referenceMonth(4)
                        .clientCount(2)
                        .scheduledAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                        .build(),
                CultivationNotificationCandidate.builder()
                        .type(NotificationType.CULTIVATION_HARVEST_FEEDBACK)
                        .userId(2L)
                        .productId(11L)
                        .productName("고추")
                        .referenceMonth(7)
                        .clientCount(1)
                        .scheduledAt(LocalDateTime.of(2026, 7, 1, 9, 0))
                        .build()
        ));

        cultivationNotificationScheduler.createCultivationNotifications();

        verify(notificationCommandService)
                .createCultivationSowingPromotion(1L, 10L, "수박", 4, 2, LocalDateTime.of(2026, 3, 1, 9, 0));
        verify(notificationCommandService)
                .createCultivationHarvestFeedback(2L, 11L, "고추", 7, 1, LocalDateTime.of(2026, 7, 1, 9, 0));
    }
}
