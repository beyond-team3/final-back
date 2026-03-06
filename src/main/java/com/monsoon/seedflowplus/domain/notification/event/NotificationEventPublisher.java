package com.monsoon.seedflowplus.domain.notification.event;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(Object event) {
        Objects.requireNonNull(event, "event must not be null");
        applicationEventPublisher.publishEvent(event);
    }
}
