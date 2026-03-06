package com.monsoon.seedflowplus.domain.notification.repository;

import com.monsoon.seedflowplus.domain.notification.entity.NotificationDelivery;
import com.monsoon.seedflowplus.domain.notification.entity.DeliveryChannel;
import com.monsoon.seedflowplus.domain.notification.entity.DeliveryStatus;
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    long deleteByNotification_Id(Long notificationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    long deleteByNotification_User_Id(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    long deleteByNotification_CreatedAtBefore(LocalDateTime cutoff);
}
