package com.monsoon.seedflowplus.domain.notification.command;

import com.monsoon.seedflowplus.domain.notification.entity.DeliveryChannel;
import com.monsoon.seedflowplus.domain.notification.entity.DeliveryStatus;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationDelivery;
import com.monsoon.seedflowplus.domain.notification.repository.NotificationDeliveryRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationDeliveryWorkerService {

    private final NotificationDeliveryRepository notificationDeliveryRepository;

    public int dispatchDueDeliveries(LocalDateTime now) {
        Objects.requireNonNull(now, "now must not be null");

        List<NotificationDelivery> dueDeliveries =
                notificationDeliveryRepository
                        .findTop100ForUpdateSkipLockedByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                        DeliveryStatus.PENDING.name(),
                        now
                );

        int processedCount = 0;
        for (NotificationDelivery delivery : dueDeliveries) {
            delivery.markAttempt(now);
            try {
                dispatch(delivery, now);
            } catch (Exception e) {
                String reason = e.getMessage() == null ? "dispatch failed" : e.getMessage();
                log.warn(
                        "Failed to dispatch notification delivery. deliveryId={}, channel={}, reason={}",
                        delivery.getId(),
                        delivery.getChannel(),
                        reason
                );
                delivery.markFailed(now, reason);
            }
            processedCount++;
        }

        return processedCount;
    }

    private void dispatch(NotificationDelivery delivery, LocalDateTime now) {
        if (delivery.getChannel() == DeliveryChannel.IN_APP) {
            delivery.markSent(now, null);
            return;
        }

        throw new IllegalStateException("Unsupported delivery channel: " + delivery.getChannel());
    }
}
