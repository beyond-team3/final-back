package com.monsoon.seedflowplus.domain.notification.command;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class NotificationSseService {

    private static final long SSE_TIMEOUT_MILLIS = 60L * 60L * 1000L;

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter connect(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        SseEmitter previousEmitter = emitters.put(userId, emitter);
        if (previousEmitter != null) {
            previousEmitter.complete();
        }

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(ex -> remove(userId, emitter));

        return emitter;
    }

    public void send(Long userId, Object payload) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            return;
        }

        try {
            emitter.send(SseEmitter.event().name("notification").data(payload));
        } catch (IOException | IllegalStateException e) {
            log.warn("SSE send failed. userId={}, reason={}", userId, e.getMessage(), e);
            remove(userId, emitter);
        }
    }

    public void remove(Long userId) {
        emitters.remove(userId);
    }

    private void remove(Long userId, SseEmitter emitter) {
        emitters.remove(userId, emitter);
    }
}
