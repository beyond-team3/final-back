package com.monsoon.seedflowplus.domain.notification.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.notification.command.NotificationCommandService;
import com.monsoon.seedflowplus.domain.notification.dto.response.NotificationListItemResponse;
import com.monsoon.seedflowplus.domain.notification.dto.response.UnreadCountResponse;
import com.monsoon.seedflowplus.domain.notification.query.NotificationQueryService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationQueryService notificationQueryService;
    private final NotificationCommandService notificationCommandService;

    @GetMapping
    public ApiResult<Page<NotificationListItemResponse>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = principal.getUserId();
        Pageable pageable = PageRequest.of(page, size);

        Page<NotificationListItemResponse> response = notificationQueryService.getMyNotifications(userId, pageable)
                .map(NotificationListItemResponse::from);
        return ApiResult.success(response);
    }

    @GetMapping("/unread-count")
    public ApiResult<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = principal.getUserId();
        long unreadCount = notificationQueryService.getUnreadCount(userId);

        return ApiResult.success(new UnreadCountResponse(unreadCount));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResult<?> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = principal.getUserId();
        notificationCommandService.markAsRead(userId, notificationId, LocalDateTime.now());

        return ApiResult.success();
    }

    @PatchMapping("/read-all")
    public ApiResult<?> markAllAsRead(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        Long userId = principal.getUserId();
        notificationCommandService.markAllAsRead(userId, LocalDateTime.now());

        return ApiResult.success();
    }
}
