package com.monsoon.seedflowplus.domain.billing.payment.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.billing.payment.dto.request.PaymentCreateRequest;
import com.monsoon.seedflowplus.domain.billing.payment.dto.response.PaymentListResponse;
import com.monsoon.seedflowplus.domain.billing.payment.dto.response.PaymentResponse;
import com.monsoon.seedflowplus.domain.billing.payment.service.PaymentService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Payment", description = "결제 API")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "결제 처리", description = "PUBLISHED 상태의 청구서를 결제합니다. 거래처만 가능합니다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<PaymentResponse> processPayment(
            @RequestBody @Valid PaymentCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResult.success(paymentService.processPayment(request, userDetails.getClientId()));
    }

    @Operation(summary = "결제 단건 조회", description = "결제 ID로 단건 조회합니다.")
    @GetMapping("/{paymentId}")
    public ApiResult<PaymentResponse> getPayment(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResult.success(
                paymentService.getPayment(paymentId, userDetails.getClientId())
        );
    }

    @Operation(summary = "결제 목록 조회 (거래처별)", description = "특정 거래처의 결제 목록을 조회합니다.")
    @GetMapping("/clients/{clientId}")
    public ApiResult<List<PaymentListResponse>> getPaymentsByClient(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResult.success(paymentService.getPaymentsByClient(userDetails.getClientId()));
    }

    @Operation(summary = "결제 목록 조회 (전체)", description = "전체 결제 목록을 조회합니다.")
    @GetMapping
    public ApiResult<List<PaymentListResponse>> getPayments() {
        return ApiResult.success(paymentService.getPayments());
    }
}