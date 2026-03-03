package com.monsoon.seedflowplus.domain.sales.request.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.sales.request.dto.request.QuotationRequestCreateRequest;
import com.monsoon.seedflowplus.domain.sales.request.dto.response.QuotationRequestListResponse;
import com.monsoon.seedflowplus.domain.sales.request.dto.response.QuotationRequestResponse;
import com.monsoon.seedflowplus.domain.sales.request.service.QuotationRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Quotation Request", description = "견적요청서(RFQ) API")
@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
public class QuotationRequestController {

    private final QuotationRequestService quotationRequestService;

    @Operation(summary = "견적요청서 작성", description = "새로운 견적요청서(RFQ)를 작성합니다. (Role: CLIENT 전용)")
    @PostMapping
    public ApiResult<?> createQuotationRequest(@RequestBody @Valid QuotationRequestCreateRequest request) {
        quotationRequestService.createQuotationRequest(request);
        return ApiResult.success();
    }

    @Operation(summary = "대기 중인 견적요청서 목록 조회", description = "견적서 작성 시 사용할 대기(PENDING) 상태의 견적요청서 목록을 조회합니다. (Role: SALES_REP 전용)")
    @GetMapping("/pending")
    public ApiResult<List<QuotationRequestListResponse>> getPendingQuotationRequests() {
        List<QuotationRequestListResponse> response = quotationRequestService.getPendingQuotationRequests();
        return ApiResult.success(response);
    }

    @Operation(summary = "견적요청서 상세 조회", description = "견적요청서 ID를 통해 상세 정보를 조회합니다. (Role별 접근 제어 적용)")
    @GetMapping("/{id}")
    public ApiResult<QuotationRequestResponse> getQuotationRequest(@PathVariable("id") Long id) {
        QuotationRequestResponse response = quotationRequestService.getQuotationRequest(id);
        return ApiResult.success(response);
    }

    @Operation(summary = "견적요청서 삭제", description = "견적요청서 ID를 통해 논리적으로 삭제합니다 (상태를 DELETED로 변경).")
    @DeleteMapping("/{id}")
    public ApiResult<?> deleteQuotationRequest(@PathVariable("id") Long id) {
        quotationRequestService.deleteQuotationRequest(id);
        return ApiResult.success();
    }
}
