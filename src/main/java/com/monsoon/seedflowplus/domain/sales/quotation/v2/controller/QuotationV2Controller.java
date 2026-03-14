package com.monsoon.seedflowplus.domain.sales.quotation.v2.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.deal.v2.dto.DealDocumentCommandResultDto;
import com.monsoon.seedflowplus.domain.sales.quotation.v2.dto.request.QuotationV2CreateRequest;
import com.monsoon.seedflowplus.domain.sales.quotation.v2.dto.request.QuotationV2ReviseRequest;
import com.monsoon.seedflowplus.domain.sales.quotation.v2.service.QuotationV2CommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Quotation V2", description = "견적서 v2 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/quotations")
public class QuotationV2Controller {

    private final QuotationV2CommandService quotationV2CommandService;

    @Operation(summary = "견적서 작성", description = "상위 문서 우선, dealId 명시, 없으면 신규 deal 생성 규칙으로 견적서를 생성합니다.")
    @PostMapping
    public ApiResult<DealDocumentCommandResultDto> createQuotation(@RequestBody @Valid QuotationV2CreateRequest request) {
        return ApiResult.success(quotationV2CommandService.createQuotation(request));
    }

    @Operation(summary = "견적서 재작성", description = "반려 또는 만료된 견적서를 새 문서로 재작성합니다.")
    @PostMapping("/{quotationId}/revise")
    public ApiResult<DealDocumentCommandResultDto> reviseQuotation(
            @PathVariable Long quotationId,
            @RequestBody @Valid QuotationV2ReviseRequest request
    ) {
        return ApiResult.success(quotationV2CommandService.reviseQuotation(quotationId, request));
    }

    @Operation(summary = "견적서 취소", description = "v2 정책 기준으로 견적서를 취소하고 deal snapshot을 재계산합니다.")
    @PatchMapping("/{quotationId}/cancel")
    public ApiResult<DealDocumentCommandResultDto> cancelQuotation(@PathVariable Long quotationId) {
        return ApiResult.success(quotationV2CommandService.cancelQuotation(quotationId));
    }
}
