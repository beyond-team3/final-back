package com.monsoon.seedflowplus.domain.sales.quotation.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.sales.quotation.dto.request.QuotationCreateRequest;
import com.monsoon.seedflowplus.domain.sales.quotation.dto.response.QuotationListResponse;
import com.monsoon.seedflowplus.domain.sales.quotation.dto.response.QuotationResponse;
import com.monsoon.seedflowplus.domain.sales.quotation.service.QuotationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Quotation", description = "견적서 API")
@RestController
@RequestMapping("/api/v1/quotations")
@RequiredArgsConstructor
public class QuotationController {

    private final QuotationService quotationService;

    @Operation(summary = "견적서 작성", description = "새로운 견적서를 작성합니다. (Role: SALES_REP 전용, 담당 거래처만 가능)")
    @PostMapping
    public ApiResult<?> createQuotation(@RequestBody @Valid QuotationCreateRequest request) {
        quotationService.createQuotation(request);
        return ApiResult.success();
    }

    @Operation(summary = "승인된 견적서 목록 조회", description = "상태가 FINAL_APPROVED인 견적서 목록을 조회합니다. (역할별 필터링 적용)")
    @GetMapping("/approved")
    public ApiResult<List<QuotationListResponse>> getApprovedQuotations() {
        List<QuotationListResponse> response = quotationService.getApprovedQuotations();
        return ApiResult.success(response);
    }

    @Operation(summary = "반려 및 만료된 견적서 목록 조회", description = "재작성을 위해 참고할 반려 또는 만료된 견적서 목록을 조회합니다.")
    @GetMapping("/rejected")
    public ApiResult<List<QuotationListResponse>> getRejectedQuotations() {
        List<QuotationListResponse> response = quotationService.getRejectedQuotations();
        return ApiResult.success(response);
    }

    @Operation(summary = "계약서 재작성 가능 견적서 목록 조회", description = "이미 계약서가 작성되었으나 모두 반려된 경우, 재작성이 가능한 상위 견적서 목록을 조회합니다.")
    @GetMapping("/rejected-contracts")
    public ApiResult<List<QuotationListResponse>> getRejectedQuotationsForContract() {
        List<QuotationListResponse> response = quotationService.getRejectedQuotationsForContract();
        return ApiResult.success(response);
    }

    @Operation(summary = "견적서 상세 조회", description = "견적서 ID를 통해 상세 정보를 조회합니다. (역할별 접근 제어 및 메모 가시성 적용)")
    @GetMapping("/{id}")
    public ApiResult<QuotationResponse> getQuotationDetail(@PathVariable("id") Long id) {
        QuotationResponse response = quotationService.getQuotationDetail(id);
        return ApiResult.success(response);
    }

    @Operation(summary = "견적서 삭제", description = "견적서 ID를 통해 논리적으로 삭제합니다 (상태를 DELETED로 변경).")
    @DeleteMapping("/{id}")
    public ApiResult<?> deleteQuotation(@PathVariable("id") Long id) {
        quotationService.deleteQuotation(id);
        return ApiResult.success();
    }
}
