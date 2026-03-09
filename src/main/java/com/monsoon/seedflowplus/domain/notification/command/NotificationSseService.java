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

        emitter.onCompletion(() -> removeIfMatch(userId, emitter));
        emitter.onTimeout(() -> removeIfMatch(userId, emitter));
        emitter.onError(ex -> removeIfMatch(userId, emitter));

        return emitter;
    }

    public boolean send(Long userId, Object payload) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            return true;
        }

        try {
            emitter.send(SseEmitter.event().name("notification").data(payload));
            return true;
        } catch (IOException | IllegalStateException e) {
            removeIfMatch(userId, emitter);
            log.warn("SSE send failed. userId={}, reason={}", userId, e.getMessage(), e);
            return false;
        }
    }

    private void removeIfMatch(Long userId, SseEmitter emitter) {
        emitters.remove(userId, emitter);
    }
}
