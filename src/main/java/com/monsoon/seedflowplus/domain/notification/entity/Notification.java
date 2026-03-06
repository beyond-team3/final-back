package com.monsoon.seedflowplus.domain.notification.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseCreateEntity;
import com.monsoon.seedflowplus.domain.account.entity.User;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "tbl_notification",
        indexes = {
                @Index(name = "idx_notification_user_created_at", columnList = "user_id, created_at"),
                @Index(name = "idx_notification_user_read_at", columnList = "user_id, read_at"),
                @Index(name = "idx_notification_target", columnList = "target_type, target_id")
        }
)
@AttributeOverride(name = "id", column = @Column(name = "notification_id"))
@SQLDelete(sql = "UPDATE tbl_notification SET is_deleted = true WHERE notification_id = ?")
@SQLRestriction("is_deleted = false")
public class Notification extends BaseCreateEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 60)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 40)
    private NotificationTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    @Builder
    public Notification(
            User user,
            NotificationType type,
            String title,
            String content,
            NotificationTargetType targetType,
            Long targetId
    ) {
        this.user = user;
        this.type = type;
        this.title = title;
        this.content = content;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public boolean isRead() {
        return readAt != null;
    }

    public void markAsRead(LocalDateTime now) {
        if (this.readAt == null) {
            this.readAt = now;
        }
    }
}
