package com.monsoon.seedflowplus.domain.sales.contract.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.sales.contract.dto.request.ContractCreateRequest;
import com.monsoon.seedflowplus.domain.sales.contract.dto.response.ContractPrefillResponse;
import com.monsoon.seedflowplus.domain.sales.contract.dto.response.ContractResponse;
import com.monsoon.seedflowplus.domain.sales.contract.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.monsoon.seedflowplus.domain.sales.contract.dto.response.ContractSimpleResponse;
import java.util.List;

@Tag(name = "Contract", description = "계약 API")
@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @Operation(summary = "거래처별 계약 목록 조회 (드롭다운용)", description = "특정 거래처 ID에 귀속된 계약 목록을 조회합니다.")
    @GetMapping
    public ApiResult<List<ContractSimpleResponse>> getContractsByClient(@RequestParam("clientId") Long clientId) {
        return ApiResult.success(contractService.getContractsByClient(clientId));
    }

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

    @Operation(summary = "계약서 상세 조회", description = "계약서 ID를 통해 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ApiResult<ContractResponse> getContractDetail(@PathVariable("id") Long id) {
        return ApiResult.success(contractService.getContractDetail(id));
    }

    @Operation(summary = "계약서 삭제", description = "계약서를 논리적으로 삭제합니다 (상태를 DELETED로 변경).")
    @DeleteMapping("/{id}")
    public ApiResult<?> deleteContract(@PathVariable("id") Long id) {
        contractService.deleteContract(id);
        return ApiResult.success();
    }
}
