package com.monsoon.seedflowplus.domain.notification.scheduler;

import com.monsoon.seedflowplus.domain.notification.command.NotificationCommandService;
import com.monsoon.seedflowplus.domain.notification.command.NotificationDeliveryWorkerService;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDeliveryScheduler {
    private final NotificationDeliveryWorkerService notificationDeliveryWorkerService;
    private final NotificationCommandService notificationCommandService;
    private final Clock clock;
    @Value("${notification.retention-days:30}")
    private long retentionDays;

    @Scheduled(fixedDelayString = "${notification.delivery.dispatch-fixed-delay-ms:60000}")
    public void dispatchDueDeliveries() {
        LocalDateTime now = LocalDateTime.now(clock);
        int processedCount = notificationDeliveryWorkerService.dispatchDueDeliveries(now);
        log.debug("Notification dispatch done. processedCount={}, now={}", processedCount, now);
    }

    @Scheduled(cron = "${notification.retention.cron:0 0 3 * * *}")
    public void deleteExpiredNotifications() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(retentionDays);
        long deletedCount = notificationCommandService.deleteOlderThan(cutoff);
        log.info("Notification retention cleanup done. cutoff={}, deletedCount={}", cutoff, deletedCount);
    }
}
