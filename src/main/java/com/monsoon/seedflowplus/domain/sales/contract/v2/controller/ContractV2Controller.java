package com.monsoon.seedflowplus.domain.sales.contract.v2.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.deal.v2.dto.DealDocumentCommandResultDto;
import com.monsoon.seedflowplus.domain.sales.contract.v2.dto.request.ContractV2CreateRequest;
import com.monsoon.seedflowplus.domain.sales.contract.v2.dto.request.ContractV2ReviseRequest;
import com.monsoon.seedflowplus.domain.sales.contract.v2.service.ContractV2CommandService;
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

@Tag(name = "Contract V2", description = "계약서 v2 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/contracts")
public class ContractV2Controller {

    private final ContractV2CommandService contractV2CommandService;

    @Operation(summary = "계약서 작성", description = "상위 견적서 우선, dealId 명시, 없으면 신규 deal 생성 규칙으로 계약서를 생성합니다.")
    @PostMapping
    public ApiResult<DealDocumentCommandResultDto> createContract(@RequestBody @Valid ContractV2CreateRequest request) {
        return ApiResult.success(contractV2CommandService.createContract(request));
    }

    @Operation(summary = "계약서 재작성", description = "반려 또는 만료된 계약서를 새 문서로 재작성합니다.")
    @PostMapping("/{contractId}/revise")
    public ApiResult<DealDocumentCommandResultDto> reviseContract(
            @PathVariable Long contractId,
            @RequestBody @Valid ContractV2ReviseRequest request
    ) {
        return ApiResult.success(contractV2CommandService.reviseContract(contractId, request));
    }

    @Operation(summary = "계약서 취소", description = "v2 정책 기준으로 계약서를 취소하고 deal snapshot을 재계산합니다.")
    @PatchMapping("/{contractId}/cancel")
    public ApiResult<DealDocumentCommandResultDto> cancelContract(@PathVariable Long contractId) {
        return ApiResult.success(contractV2CommandService.cancelContract(contractId));
    }
}
