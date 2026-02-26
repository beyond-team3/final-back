/*
package com.monsoon.seedflowplus.domain.sales.order.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.sales.order.dto.request.OrderCreateRequest;
import com.monsoon.seedflowplus.domain.sales.order.dto.response.OrderCancelResponse;
import com.monsoon.seedflowplus.domain.sales.order.dto.response.OrderListResponse;
import com.monsoon.seedflowplus.domain.sales.order.dto.response.OrderResponse;
import com.monsoon.seedflowplus.domain.sales.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Order", description = "주문 API")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 생성", description = "계약 기반으로 주문을 생성합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<OrderResponse> createOrder(
            @RequestBody @Valid OrderCreateRequest request,
            @RequestParam Long clientId
    ) {
        return ApiResult.success(orderService.createOrder(request, clientId));
    }

    @Operation(summary = "주문 목록 조회", description = "거래처의 주문 목록을 조회합니다.")
    @GetMapping
    public ApiResult<List<OrderListResponse>> getOrders(
            @RequestParam Long clientId
    ) {
        return ApiResult.success(orderService.getOrders(clientId));
    }

    @Operation(summary = "주문 단건 조회", description = "주문 ID로 단건 조회합니다.")
    @GetMapping("/{orderId}")
    public ApiResult<OrderResponse> getOrder(
            @PathVariable Long orderId
    ) {
        return ApiResult.success(orderService.getOrder(orderId));
    }

    @Operation(summary = "주문 취소", description = "주문 상태를 CANCELED로 변경합니다.")
    @PatchMapping("/{orderId}/cancel")
    public ApiResult<OrderCancelResponse> cancelOrder(
            @PathVariable Long orderId,
            @RequestParam Long clientId
    ) {
        return ApiResult.success(orderService.cancelOrder(orderId, clientId));
    }

    @Operation(summary = "주문 확정", description = "주문 상태를 CONFIRMED로 변경하고 명세서를 자동 발급합니다.")
    @PatchMapping("/{orderId}/confirm")
    public ApiResult<OrderResponse> confirmOrder(
            @PathVariable Long orderId
    ) {
        return ApiResult.success(orderService.confirmOrder(orderId));
    }
}
*/
