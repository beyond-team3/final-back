package com.monsoon.seedflowplus.domain.notification.repository;

import com.monsoon.seedflowplus.domain.notification.entity.Notification;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationTargetType;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    boolean existsByUser_IdAndTypeAndTargetTypeAndTargetIdAndCreatedAtBetween(
            Long userId,
            NotificationType type,
            NotificationTargetType targetType,
            Long targetId,
            LocalDateTime from,
            LocalDateTime to
    );

    boolean existsByUser_IdAndTypeAndTargetTypeAndTargetIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long userId,
            NotificationType type,
            NotificationTargetType targetType,
            Long targetId,
            LocalDateTime from,
            LocalDateTime to
    );

    boolean existsByUser_IdAndTypeAndTargetTypeAndTargetIdAndContentAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long userId,
            NotificationType type,
            NotificationTargetType targetType,
            Long targetId,
            String content,
            LocalDateTime from,
            LocalDateTime to
    );

    @Query("""
            SELECT n
            FROM Notification n
            WHERE n.user.id = :userId
              AND EXISTS (
                  SELECT 1
                  FROM NotificationDelivery d
                  WHERE d.notification = n
                    AND d.status = com.monsoon.seedflowplus.domain.notification.entity.DeliveryStatus.SENT
              )
            ORDER BY n.createdAt DESC
            """)
    Page<Notification> findVisibleByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT COUNT(n)
            FROM Notification n
            WHERE n.user.id = :userId
              AND n.readAt IS NULL
              AND EXISTS (
                  SELECT 1
                  FROM NotificationDelivery d
                  WHERE d.notification = n
                    AND d.status = com.monsoon.seedflowplus.domain.notification.entity.DeliveryStatus.SENT
              )
            """)
    long countVisibleUnreadByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT n
            FROM Notification n
            WHERE n.id = :id
              AND n.user.id = :userId
              AND EXISTS (
                  SELECT 1
                  FROM NotificationDelivery d
                  WHERE d.notification = n
                    AND d.status = com.monsoon.seedflowplus.domain.notification.entity.DeliveryStatus.SENT
              )
            """)
    Optional<Notification> findVisibleByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    Optional<Notification> findByIdAndUser_Id(Long id, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Notification n
            set n.readAt = :now
            where n.user.id = :userId
              and n.readAt is null
              and exists (
                  select 1
                  from NotificationDelivery d
                  where d.notification = n
                    and d.status = com.monsoon.seedflowplus.domain.notification.entity.DeliveryStatus.SENT
              )
            """)
    int markAllVisibleAsRead(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Notification n
            set n.isDeleted = true
            where n.user.id = :userId
              and n.isDeleted = false
              and exists (
                  select 1
                  from NotificationDelivery d
                  where d.notification = n
                    and d.status = com.monsoon.seedflowplus.domain.notification.entity.DeliveryStatus.SENT
              )
            """)
    int deleteVisibleByUser_Id(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.isDeleted = true where n.createdAt < :cutoff and n.isDeleted = false")
    int deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
