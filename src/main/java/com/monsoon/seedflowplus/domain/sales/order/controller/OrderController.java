/*
package com.monsoon.seedflowplus.domain.document.order.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.document.order.dto.request.OrderCreateRequest;
import com.monsoon.seedflowplus.domain.document.order.dto.response.OrderCancelResponse;
import com.monsoon.seedflowplus.domain.document.order.dto.response.OrderListResponse;
import com.monsoon.seedflowplus.domain.document.order.dto.response.OrderResponse;
import com.monsoon.seedflowplus.domain.document.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Order", description = "주문 API")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 생성", description = "계약 기반으로 주문을 생성합니다. 거래처만 가능합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CLIENT')")
    public ApiResult<OrderResponse> createOrder(
            @RequestBody @Valid OrderCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResult.success(orderService.createOrder(request, principal.getClientId()));
    }

    @Operation(summary = "주문 목록 조회", description = "로그인한 거래처의 주문 목록을 조회합니다.")
    @GetMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ApiResult<List<OrderListResponse>> getOrders(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResult.success(orderService.getOrders(principal.getClientId()));
    }

    @Operation(summary = "주문 단건 조회", description = "주문 ID로 단건 조회합니다.")
    @GetMapping("/{orderId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ApiResult<OrderResponse> getOrder(
            @PathVariable Long orderId
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResult.success(orderService.getOrder(orderId));
    }

    @Operation(summary = "주문 취소", description = "주문 상태를 canceled로 변경합니다.")
    @PatchMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('CLIENT')")
    public ApiResult<OrderCancelResponse> cancelOrder(
            @PathVariable Long orderId
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResult.success(orderService.cancelOrder(orderId, principal.getClientId()));
    }
}*/
