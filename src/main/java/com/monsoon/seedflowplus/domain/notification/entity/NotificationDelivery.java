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
                @Index(name = "idx_delivery_channel_status", columnList = "channel, status")
        }
)
@AttributeOverride(name = "id", column = @Column(name = "delivery_id"))
public class NotificationDelivery extends BaseModifyEntity {

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

    @Builder
    public NotificationDelivery(Notification notification, DeliveryChannel channel, DeliveryStatus status) {
        this.notification = notification;
        this.channel = channel;
        this.status = status;
        this.attemptCount = 0;
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
        this.failReason = reason;
    }
}
