package com.monsoon.seedflowplus.domain.notification.event;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    void publish(Object event) {
        Objects.requireNonNull(event, "event must not be null");
        applicationEventPublisher.publishEvent(event);
    }

    public void publishAfterCommit(Object event) {
        Objects.requireNonNull(event, "event must not be null");
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            applicationEventPublisher.publishEvent(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    applicationEventPublisher.publishEvent(event);
                } catch (Exception e) {
                    log.error("Failed to publish notification event after transaction commit. event={}", event, e);
                }
            }
        });
    }
}
