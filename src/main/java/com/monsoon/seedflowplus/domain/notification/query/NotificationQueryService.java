package com.monsoon.seedflowplus.domain.notification.query;

import com.monsoon.seedflowplus.domain.notification.entity.Notification;
import com.monsoon.seedflowplus.domain.notification.repository.NotificationRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;

    public Page<Notification> getMyNotifications(Long userId, Pageable pageable) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(pageable, "pageable must not be null");
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable);
    }

    public long getUnreadCount(Long userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return notificationRepository.countByUser_IdAndReadAtIsNull(userId);
    }
}
