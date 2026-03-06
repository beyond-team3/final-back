package com.monsoon.seedflowplus.domain.notification.command;

import com.monsoon.seedflowplus.domain.notification.entity.DeliveryChannel;
import com.monsoon.seedflowplus.domain.notification.entity.DeliveryStatus;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationDelivery;
import com.monsoon.seedflowplus.domain.notification.dto.response.NotificationListItemResponse;
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

    private static final int FAIL_REASON_MAX_LENGTH = 500;

    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final NotificationSseService notificationSseService;

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
                String reason = normalizeReason(e);
                log.warn(
                        "Failed to dispatch notification delivery. deliveryId={}, channel={}, reason={}",
                        delivery.getId(),
                        delivery.getChannel(),
                        reason,
                        e
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
            notificationSseService.send(
                    delivery.getNotification().getUser().getId(),
                    NotificationListItemResponse.from(delivery.getNotification())
            );
            return;
        }

        throw new IllegalStateException("Unsupported delivery channel: " + delivery.getChannel());
    }

    private String normalizeReason(Exception e) {
        String message = e.getMessage() == null ? "dispatch failed" : e.getMessage().trim();
        if (message.isEmpty()) {
            message = "dispatch failed";
        }
        if (message.length() > FAIL_REASON_MAX_LENGTH) {
            return message.substring(0, FAIL_REASON_MAX_LENGTH);
        }
        return message;
    }
}
