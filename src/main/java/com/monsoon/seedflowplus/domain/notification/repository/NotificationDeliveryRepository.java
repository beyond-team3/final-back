package com.monsoon.seedflowplus.domain.notification.repository;

import com.monsoon.seedflowplus.domain.notification.entity.NotificationDelivery;
import com.monsoon.seedflowplus.domain.notification.entity.DeliveryChannel;
import com.monsoon.seedflowplus.domain.notification.entity.DeliveryStatus;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationTargetType;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    List<NotificationDelivery> findTop100ByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            DeliveryStatus status,
            LocalDateTime now
    );

    List<NotificationDelivery> findTop100ByChannelAndStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            DeliveryChannel channel,
            DeliveryStatus status,
            LocalDateTime now
    );

    /**
     * MariaDB 10.6+ 환경에서만 {@code FOR UPDATE SKIP LOCKED}를 사용해 due delivery를 선점한다.
     * 하위 버전 호환은 서비스 계층 fallback 조회 경로로 보완한다.
     */
    @Query(
            value = """
                    SELECT delivery_id
                    FROM tbl_notification_delivery
                    WHERE status = :status
                      AND scheduled_at <= :now
                    ORDER BY scheduled_at ASC
                    LIMIT 100
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<Long> findTop100IdsForUpdateSkipLockedByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            @Param("status") String status,
            @Param("now") LocalDateTime now
    );

    @Query("""
            SELECT d
            FROM NotificationDelivery d
            JOIN FETCH d.notification n
            JOIN FETCH n.user u
            WHERE d.id IN :deliveryIds
            ORDER BY d.scheduledAt ASC
            """)
    List<NotificationDelivery> findAllWithNotificationAndUserByIdInOrderByScheduledAtAsc(
            @Param("deliveryIds") List<Long> deliveryIds
    );

    @Query("""
            SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END
            FROM NotificationDelivery d
            JOIN d.notification n
            WHERE n.user.id = :userId
              AND n.type = :type
              AND n.targetType = :targetType
              AND n.targetId = :targetId
              AND d.scheduledAt = :scheduledAt
            """)
    boolean existsByNotification_UserIdAndNotification_TypeAndNotification_TargetTypeAndNotification_TargetIdAndScheduledAt(
            @Param("userId") Long userId,
            @Param("type") NotificationType type,
            @Param("targetType") NotificationTargetType targetType,
            @Param("targetId") Long targetId,
            @Param("scheduledAt") LocalDateTime scheduledAt
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    UPDATE tbl_notification_delivery d
                    JOIN tbl_notification n ON n.notification_id = d.notification_id
                    SET d.is_deleted = true
                    WHERE n.user_id = :userId
                      AND d.is_deleted = false
                      AND EXISTS (
                          SELECT 1
                          FROM tbl_notification_delivery visible_d
                          WHERE visible_d.notification_id = n.notification_id
                            AND visible_d.status = 'SENT'
                            AND visible_d.is_deleted = false
                      )
                    """,
            nativeQuery = true
    )
    int deleteVisibleByNotification_User_Id(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    UPDATE tbl_notification_delivery
                    SET is_deleted = true
                    WHERE notification_id = :notificationId
                      AND is_deleted = false
                    """,
            nativeQuery = true
    )
    int deleteByNotification_Id(@Param("notificationId") Long notificationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    UPDATE tbl_notification_delivery d
                    JOIN tbl_notification n ON n.notification_id = d.notification_id
                    SET d.is_deleted = true
                    WHERE n.user_id = :userId
                      AND d.is_deleted = false
                    """,
            nativeQuery = true
    )
    int deleteByNotification_User_Id(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    UPDATE tbl_notification_delivery d
                    JOIN tbl_notification n ON n.notification_id = d.notification_id
                    SET d.is_deleted = true
                    WHERE n.created_at < :cutoff
                      AND d.is_deleted = false
                    """,
            nativeQuery = true
    )
    int deleteByNotification_CreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
