package com.monsoon.seedflowplus.domain.notification.repository;

import com.monsoon.seedflowplus.domain.notification.entity.NotificationDelivery;
import com.monsoon.seedflowplus.domain.notification.entity.DeliveryChannel;
import com.monsoon.seedflowplus.domain.notification.entity.DeliveryStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
