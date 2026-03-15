package com.monsoon.seedflowplus.domain.notification.scheduler;

import com.monsoon.seedflowplus.domain.notification.command.NotificationCommandService;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationType;
import com.monsoon.seedflowplus.domain.product.service.CultivationNotificationCandidate;
import com.monsoon.seedflowplus.domain.product.service.ProductCultivationAlertService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CultivationNotificationScheduler {

    private final ProductCultivationAlertService productCultivationAlertService;
    private final NotificationCommandService notificationCommandService;
    private final Clock clock;

    @Scheduled(cron = "${notification.cultivation.cron:0 0 1 * * *}")
    public void createCultivationNotifications() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<CultivationNotificationCandidate> candidates = productCultivationAlertService.getNotificationCandidates(now);

        for (CultivationNotificationCandidate candidate : candidates) {
            if (candidate.getType() == NotificationType.CULTIVATION_SOWING_PROMOTION) {
                notificationCommandService.createCultivationSowingPromotion(
                        candidate.getUserId(),
                        candidate.getProductId(),
                        candidate.getProductName(),
                        candidate.getReferenceMonth(),
                        candidate.getClientCount(),
                        candidate.getScheduledAt());
                continue;
            }

            notificationCommandService.createCultivationHarvestFeedback(
                    candidate.getUserId(),
                    candidate.getProductId(),
                    candidate.getProductName(),
                    candidate.getReferenceMonth(),
                    candidate.getClientCount(),
                    candidate.getScheduledAt());
        }

        log.info("Cultivation notification scheduling done. now={}, candidateCount={}", now, candidates.size());
    }
}
