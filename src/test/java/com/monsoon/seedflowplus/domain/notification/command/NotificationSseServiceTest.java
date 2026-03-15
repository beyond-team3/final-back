package com.monsoon.seedflowplus.domain.notification.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class NotificationSseServiceTest {

    @Test
    void sendLogsBrokenPipeAtDebugAndRemovesEmitter() throws Exception {
        NotificationSseService service = new NotificationSseService();
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new AsyncRequestNotUsableException("ServletOutputStream failed to flush: java.io.IOException: Broken pipe"))
                .when(emitter)
                .send(org.mockito.ArgumentMatchers.any(SseEmitter.SseEventBuilder.class));

        @SuppressWarnings("unchecked")
        Map<Long, SseEmitter> emitters = (Map<Long, SseEmitter>) ReflectionTestUtils.getField(service, "emitters");
        assertThat(emitters).isNotNull();
        emitters.put(5L, emitter);

        Logger logger = (Logger) LoggerFactory.getLogger(NotificationSseService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);

        try {
            boolean sent = service.send(5L, "payload");

            assertThat(sent).isFalse();
            assertThat(emitters).doesNotContainKey(5L);
            assertThat(appender.list)
                    .anySatisfy(event -> {
                        assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
                        assertThat(event.getFormattedMessage()).contains("SSE client disconnected");
                    });
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void sendLogsUnexpectedIoExceptionAtWarn() throws Exception {
        NotificationSseService service = new NotificationSseService();
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IOException("stream closed unexpectedly"))
                .when(emitter)
                .send(org.mockito.ArgumentMatchers.any(SseEmitter.SseEventBuilder.class));

        @SuppressWarnings("unchecked")
        Map<Long, SseEmitter> emitters = (Map<Long, SseEmitter>) ReflectionTestUtils.getField(service, "emitters");
        assertThat(emitters).isNotNull();
        emitters.put(7L, emitter);

        Logger logger = (Logger) LoggerFactory.getLogger(NotificationSseService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);

        try {
            boolean sent = service.send(7L, "payload");

            assertThat(sent).isFalse();
            assertThat(appender.list)
                    .anySatisfy(event -> {
                        assertThat(event.getLevel()).isEqualTo(Level.WARN);
                        assertThat(event.getFormattedMessage()).contains("SSE send failed");
                    });
        } finally {
            logger.detachAppender(appender);
        }
    }
}
