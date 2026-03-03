package com.monsoon.seedflowplus.domain.billing.invoice.controller;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.billing.invoice.dto.request.InvoiceCreateRequest;
import com.monsoon.seedflowplus.domain.billing.invoice.dto.response.InvoiceDetailResponse;
import com.monsoon.seedflowplus.domain.billing.invoice.dto.response.InvoiceListResponse;
import com.monsoon.seedflowplus.domain.billing.invoice.dto.response.InvoicePublishResponse;
import com.monsoon.seedflowplus.domain.billing.invoice.dto.response.InvoiceResponse;
import com.monsoon.seedflowplus.domain.billing.invoice.service.InvoiceService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Invoice", description = "청구서 API")
@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @Operation(summary = "청구서 수동 생성", description = "영업사원이 청구서를 수동으로 생성합니다.")
    @PostMapping
    public ApiResult<InvoiceDetailResponse> createInvoice(
            @RequestBody @Valid InvoiceCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        if (userDetails == null || userDetails.getEmployeeId() == null) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        return ApiResult.success(
                invoiceService.createInvoice(request, userDetails.getEmployeeId())
        );
    }


    @Operation(summary = "청구서 발행 확정", description = "DRAFT 상태의 청구서를 PUBLISHED로 변경합니다.")
    @PatchMapping("/{invoiceId}/publish")
    public ApiResult<InvoicePublishResponse> publishInvoice(
            @PathVariable Long invoiceId
    ) {
        return ApiResult.success(invoiceService.publishInvoice(invoiceId));
    }

    @Operation(summary = "명세서 포함/제외 토글", description = "청구서에 포함된 명세서를 포함/제외 처리합니다. DRAFT 상태에서만 가능합니다.")
    @PatchMapping("/{invoiceId}/statements/{statementId}/toggle")
    public ApiResult<InvoiceDetailResponse> toggleStatement(
            @PathVariable Long invoiceId,
            @PathVariable Long statementId
    ) {
        return ApiResult.success(invoiceService.toggleStatement(invoiceId, statementId));
    }

    @Operation(summary = "청구서 단건 조회 (공통)", description = "청구서를 조회합니다. memo는 포함되지 않습니다.")
    @GetMapping("/{invoiceId}")
    public ApiResult<InvoiceResponse> getInvoice(
            @PathVariable Long invoiceId
    ) {
        return ApiResult.success(invoiceService.getInvoice(invoiceId));
    }

    @Operation(summary = "청구서 단건 조회 (영업사원)", description = "memo를 포함한 청구서 상세를 조회합니다. 영업사원 전용입니다.")
    @GetMapping("/{invoiceId}/detail")
    public ApiResult<InvoiceDetailResponse> getInvoiceDetail(
            @PathVariable Long invoiceId
    ) {
        return ApiResult.success(invoiceService.getInvoiceDetail(invoiceId));
    }

    @Operation(summary = "청구서 목록 조회 (전체)", description = "전체 청구서 목록을 조회합니다.")
    @GetMapping
    public ApiResult<List<InvoiceListResponse>> getInvoices() {
        return ApiResult.success(invoiceService.getInvoices());
    }

    @Operation(summary = "청구서 목록 조회 (거래처별)", description = "특정 거래처의 청구서 목록을 조회합니다.")
    @GetMapping("/clients/me")
    public ApiResult<List<InvoiceListResponse>> getInvoicesByClient(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResult.success(invoiceService.getInvoicesByClient(requireClientId(userDetails)));
    }

    private Long requireEmployeeId(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getEmployeeId() == null)
            throw new CoreException(ErrorType.ACCESS_DENIED);
        return userDetails.getEmployeeId();
    }

    private Long requireClientId(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getClientId() == null)
            throw new CoreException(ErrorType.ACCESS_DENIED);
        return userDetails.getClientId();
    }
}