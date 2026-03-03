package com.monsoon.seedflowplus.domain.sales.quotation.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.sales.quotation.dto.request.QuotationCreateRequest;
import com.monsoon.seedflowplus.domain.sales.quotation.service.QuotationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
