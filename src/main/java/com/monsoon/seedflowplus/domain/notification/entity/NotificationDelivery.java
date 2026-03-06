package com.monsoon.seedflowplus.domain.notification.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "tbl_notification_delivery",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_delivery_notification_channel",
                        columnNames = {"notification_id", "channel"}
                )
        },
        indexes = {
                @Index(name = "idx_delivery_notification", columnList = "notification_id"),
                @Index(name = "idx_delivery_status", columnList = "status"),
                @Index(name = "idx_delivery_channel_status", columnList = "channel, status"),
                @Index(name = "idx_delivery_status_scheduled_at", columnList = "status, scheduled_at"),
                @Index(name = "idx_delivery_channel_status_scheduled_at", columnList = "channel, status, scheduled_at")
        }
)
@AttributeOverride(name = "id", column = @Column(name = "delivery_id"))
public class NotificationDelivery extends BaseModifyEntity {

    private static final int FAIL_REASON_MAX_LENGTH = 500;
    private static final String DEFAULT_FAIL_REASON = "dispatch failed";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "provider_message_id", length = 120)
    private String providerMessageId;

    @Column(name = "fail_reason", length = 500)
    private String failReason;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Builder
    public NotificationDelivery(
            Notification notification,
            DeliveryChannel channel,
            DeliveryStatus status,
            LocalDateTime scheduledAt
    ) {
        this.notification = Objects.requireNonNull(notification, "notification must not be null");
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.attemptCount = 0;
        this.scheduledAt = Objects.requireNonNull(scheduledAt, "scheduledAt must not be null");
    }

    public void markAttempt(LocalDateTime now) {
        this.attemptCount += 1;
        this.lastAttemptAt = now;
    }

    public void markSent(LocalDateTime now, String providerMessageId) {
        this.status = DeliveryStatus.SENT;
        this.sentAt = now;
        this.providerMessageId = providerMessageId;
        this.failReason = null;
    }

    public void markFailed(LocalDateTime now, String reason) {
        this.status = DeliveryStatus.FAILED;
        this.lastAttemptAt = now;
        this.failReason = normalizeReason(reason);
    }

    private String normalizeReason(String reason) {
        if (reason == null) {
            return DEFAULT_FAIL_REASON;
        }
        String normalized = reason.trim();
        if (normalized.isEmpty()) {
            return DEFAULT_FAIL_REASON;
        }
        if (normalized.length() > FAIL_REASON_MAX_LENGTH) {
            return normalized.substring(0, FAIL_REASON_MAX_LENGTH);
        }
        return normalized;
    }
}
