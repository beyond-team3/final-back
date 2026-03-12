package com.monsoon.seedflowplus.domain.sales.order.controller;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import com.monsoon.seedflowplus.domain.sales.order.dto.request.OrderCreateRequest;
import com.monsoon.seedflowplus.domain.sales.order.dto.response.OrderCancelResponse;
import com.monsoon.seedflowplus.domain.sales.order.dto.response.OrderListResponse;
import com.monsoon.seedflowplus.domain.sales.order.dto.response.OrderResponse;
import com.monsoon.seedflowplus.domain.sales.order.dto.response.OrderTradeSummaryResponse;
import com.monsoon.seedflowplus.domain.sales.order.service.OrderService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Order", description = "주문 API")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final ContractRepository contractHeaderRepository; // 임시 추가

    @Operation(summary = "주문 생성", description = "계약 기반으로 주문을 생성합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<OrderResponse> createOrder(
            @RequestBody @Valid OrderCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResult.success(orderService.createOrder(request, userDetails.getClientId()));
    }

    @Operation(summary = "주문 목록 조회", description = "거래처의 주문 목록을 조회합니다.")
    @GetMapping
    public ApiResult<List<OrderListResponse>> getOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResult.success(orderService.getOrders(userDetails.getClientId()));
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
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResult.success(orderService.cancelOrder(orderId, userDetails.getClientId()));
    }

    @Operation(summary = "주문 확정", description = "주문 상태를 CONFIRMED로 변경하고 명세서를 자동 발급합니다.")
    @PatchMapping("/{orderId}/confirm")
    public ApiResult<OrderResponse> confirmOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
        return ApiResult.success(orderService.confirmOrder(orderId, userDetails));
    }

    @Operation(
            summary = "거래처 거래 요약 조회",
            description = "영업사원/관리자가 특정 거래처의 이번달 거래 요약 및 여신 정보를 조회합니다."
    )
    @GetMapping("/clients/{clientId}/trade-summary")
    public ApiResult<OrderTradeSummaryResponse> getTradeSummary(
            @PathVariable Long clientId
    ) {
        return ApiResult.success(orderService.getTradeSummary(clientId));
    }


    @GetMapping("/test/contract/{id}")
    public ApiResult<String> testContract(@PathVariable Long id) {
        ContractHeader contract = contractHeaderRepository.findById(id)
                .orElse(null);
        if (contract == null) {
            return ApiResult.success("NOT FOUND - id: " + id);
        }
        return ApiResult.success("FOUND - id: " + contract.getId() + ", code: " + contract.getContractCode() + ", status: " + contract.getStatus());
    }
}
