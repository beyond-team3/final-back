package com.monsoon.seedflowplus.domain.notification.controller;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.notification.command.NotificationCommandService;
import com.monsoon.seedflowplus.domain.notification.command.NotificationSseService;
import com.monsoon.seedflowplus.domain.notification.dto.response.NotificationListItemResponse;
import com.monsoon.seedflowplus.domain.notification.dto.response.UnreadCountResponse;
import com.monsoon.seedflowplus.domain.notification.query.NotificationQueryService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Validated
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationQueryService notificationQueryService;
    private final NotificationCommandService notificationCommandService;
    private final NotificationSseService notificationSseService;
    private final Clock clock;

    @GetMapping
    public ApiResult<Page<NotificationListItemResponse>> getMyNotifications(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = resolveUserId(principal);
        Pageable pageable = PageRequest.of(page, size);

        Page<NotificationListItemResponse> response = notificationQueryService.getMyNotifications(userId, pageable)
                .map(NotificationListItemResponse::from);
        return ApiResult.success(response);
    }

    @GetMapping("/unread-count")
    public ApiResult<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = resolveUserId(principal);
        long unreadCount = notificationQueryService.getUnreadCount(userId);

        return ApiResult.success(new UnreadCountResponse(unreadCount));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResult<?> markAsRead(
            @PathVariable @Positive Long notificationId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = resolveUserId(principal);
        notificationCommandService.markAsRead(userId, notificationId, LocalDateTime.now(clock));

        return ApiResult.success();
    }

    @PatchMapping("/read-all")
    public ApiResult<?> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = resolveUserId(principal);
        notificationCommandService.markAllAsRead(userId, LocalDateTime.now(clock));

        return ApiResult.success();
    }

    @DeleteMapping("/{notificationId}")
    public ApiResult<?> deleteOne(
            @PathVariable @Positive Long notificationId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = resolveUserId(principal);
        notificationCommandService.deleteOne(userId, notificationId);
        return ApiResult.success();
    }

    @DeleteMapping
    public ApiResult<?> deleteAll(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = resolveUserId(principal);
        notificationCommandService.deleteAll(userId);
        return ApiResult.success();
    }

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = resolveUserId(principal);
        return notificationSseService.connect(userId);
    }

    private Long resolveUserId(CustomUserDetails principal) {
        if (principal == null || principal.getUserId() == null) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
        return principal.getUserId();
    }
}
