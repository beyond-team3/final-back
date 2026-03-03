package com.monsoon.seedflowplus.domain.sales.contract.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.sales.contract.dto.request.ContractCreateRequest;
import com.monsoon.seedflowplus.domain.sales.contract.dto.response.ContractPrefillResponse;
import com.monsoon.seedflowplus.domain.sales.contract.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Contract", description = "계약 API")
@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @Operation(summary = "계약서 작성을 위한 견적 데이터 불러오기", description = "견적서 ID를 통해 계약서 작성에 필요한 기본 정보를 조회합니다.")
    @GetMapping("/prefill")
    public ApiResult<ContractPrefillResponse> getPrefillData(@RequestParam("quotationId") Long quotationId) {
        return ApiResult.success(contractService.getPrefillData(quotationId));
    }

    @Operation(summary = "계약서 저장", description = "새로운 계약서를 저장합니다.")
    @PostMapping
    public ApiResult<?> createContract(@RequestBody @Valid ContractCreateRequest request) {
        contractService.createContract(request);
        return ApiResult.success();
    }
}
