package com.monsoon.seedflowplus.domain.notification.repository;

import com.monsoon.seedflowplus.domain.notification.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {
}
