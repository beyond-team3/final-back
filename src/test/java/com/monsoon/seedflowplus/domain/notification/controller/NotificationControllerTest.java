package com.monsoon.seedflowplus.domain.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.monsoon.seedflowplus.config.TestSecurityConfig;
import com.monsoon.seedflowplus.domain.notification.command.NotificationCommandService;
import com.monsoon.seedflowplus.domain.notification.command.NotificationSseService;
import com.monsoon.seedflowplus.domain.notification.dto.response.NotificationListItemResponse;
import com.monsoon.seedflowplus.domain.notification.entity.Notification;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationTargetType;
import com.monsoon.seedflowplus.domain.notification.entity.NotificationType;
import com.monsoon.seedflowplus.domain.notification.query.NotificationQueryService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@WebMvcTest(controllers = NotificationController.class)
@Import(TestSecurityConfig.class)
class NotificationControllerTest {

    private static final String BASE_PATH = "/api/v1/notifications";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationQueryService notificationQueryService;

    @MockBean
    private NotificationCommandService notificationCommandService;

    @MockBean
    private NotificationSseService notificationSseService;

    @MockBean
    private Clock clock;

    @Test
    @DisplayName("알림 목록 조회는 성공 응답을 반환한다")
    void getMyNotifications() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 3, 6, 10, 0);
        Notification notification = notification(1L, now);
        when(notificationQueryService.getMyNotifications(eq(100L), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(notification)));

        mockMvc.perform(get(BASE_PATH)
                        .with(authentication(auth(principal(100L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.content[0].type").value(NotificationType.CULTIVATION_SOWING_PROMOTION.name()));
    }

    @Test
    @DisplayName("미읽음 수 조회는 성공 응답을 반환한다")
    void getUnreadCount() throws Exception {
        when(notificationQueryService.getUnreadCount(100L)).thenReturn(7L);

        mockMvc.perform(get(BASE_PATH + "/unread-count")
                        .with(authentication(auth(principal(100L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.unreadCount").value(7));
    }

    @Test
    @DisplayName("단건 읽음 처리 시 Clock 기준 now를 서비스로 전달한다")
    void markAsRead() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 3, 6, 11, 30);
        when(clock.instant()).thenReturn(now.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        mockMvc.perform(patch(BASE_PATH + "/{notificationId}/read", 55L)
                        .with(authentication(auth(principal(100L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));

        verify(notificationCommandService).markAsRead(100L, 55L, now);
    }

    @Test
    @DisplayName("전체 읽음 처리 시 Clock 기준 now를 서비스로 전달한다")
    void markAllAsRead() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 3, 6, 11, 31);
        when(clock.instant()).thenReturn(now.atZone(ZoneId.systemDefault()).toInstant());
        when(clock.getZone()).thenReturn(ZoneId.systemDefault());

        mockMvc.perform(patch(BASE_PATH + "/read-all")
                        .with(authentication(auth(principal(100L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));

        verify(notificationCommandService).markAllAsRead(100L, now);
    }

    @Test
    @DisplayName("단건 삭제 요청은 deleteOne을 호출한다")
    void deleteOne() throws Exception {
        mockMvc.perform(delete(BASE_PATH + "/{notificationId}", 55L)
                        .with(authentication(auth(principal(100L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));

        verify(notificationCommandService).deleteOne(100L, 55L);
    }

    @Test
    @DisplayName("전체 삭제 요청은 deleteAll을 호출한다")
    void deleteAll() throws Exception {
        mockMvc.perform(delete(BASE_PATH)
                        .with(authentication(auth(principal(100L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"));

        verify(notificationCommandService).deleteAll(100L);
    }

    @Test
    @DisplayName("SSE 구독은 비동기 응답으로 emitter를 반환한다")
    void subscribe() throws Exception {
        SseEmitter emitter = new SseEmitter(1000L);
        when(notificationSseService.connect(100L)).thenReturn(emitter);

        MvcResult mvcResult = mockMvc.perform(get(BASE_PATH + "/subscribe")
                        .with(authentication(auth(principal(100L)))))
                .andExpect(request().asyncStarted())
                .andReturn();

        emitter.complete();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk());

        verify(notificationSseService).connect(100L);
    }

    @Test
    @DisplayName("비인증 요청은 401을 반환한다")
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get(BASE_PATH)).andExpect(status().isUnauthorized());
    }

    private Notification notification(Long id, LocalDateTime createdAt) {
        Notification notification = Notification.builder()
                .user(Mockito.mock(com.monsoon.seedflowplus.domain.account.entity.User.class))
                .type(NotificationType.CULTIVATION_SOWING_PROMOTION)
                .title("title")
                .content("content")
                .targetType(NotificationTargetType.PRODUCT)
                .targetId(10L)
                .build();
        ReflectionTestUtils.setField(notification, "id", id);
        ReflectionTestUtils.setField(notification, "createdAt", createdAt);
        return notification;
    }

    private CustomUserDetails principal(Long userId) {
        CustomUserDetails principal = Mockito.mock(CustomUserDetails.class);
        when(principal.getUserId()).thenReturn(userId);
        return principal;
    }

    private UsernamePasswordAuthenticationToken auth(CustomUserDetails principal) {
        return new UsernamePasswordAuthenticationToken(
                principal,
                "N/A",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }
}
