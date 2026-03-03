package com.monsoon.seedflowplus.domain.sales.request.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.sales.request.dto.request.QuotationRequestCreateRequest;
import com.monsoon.seedflowplus.domain.sales.request.service.QuotationRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

}
