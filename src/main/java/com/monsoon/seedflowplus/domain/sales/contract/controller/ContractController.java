package com.monsoon.seedflowplus.domain.sales.contract.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.sales.contract.dto.request.ContractCreateRequest;
import com.monsoon.seedflowplus.domain.sales.contract.dto.response.ContractPrefillResponse;
import com.monsoon.seedflowplus.domain.sales.contract.dto.response.ContractResponse;
import com.monsoon.seedflowplus.domain.sales.contract.dto.response.ContractListResponse;
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

    @Operation(summary = "거래처별 계약 목록 조회 (일반 조회용)", description = "거래처 ID에 귀속된 계약 목록을 조회합니다. 권한에 따라 특정 상태가 제한될 수 있습니다.")
    @GetMapping
    public ApiResult<List<ContractSimpleResponse>> getContractsByClient(@RequestParam("clientId") Long clientId) {
        return ApiResult.success(contractService.getContractsByClient(clientId));
    }

    @Operation(summary = "거래처별 활성 계약 목록 조회 (주문서 작성용)", description = "주문서 작성 시 사용 가능한 ACTIVE_CONTRACT 상태의 계약만 조회합니다.")
    @GetMapping("/active")
    public ApiResult<List<ContractSimpleResponse>> getActiveContractsByClient(@RequestParam("clientId") Long clientId) {
        return ApiResult.success(contractService.getActiveContractsByClient(clientId));
    }

    @Operation(summary = "계약서 작성을 위한 데이터 불러오기", description = "견적서 ID 또는 계약서 ID를 통해 계약서 작성에 필요한 기본 정보를 조회합니다.")
    @GetMapping("/prefill")
    public ApiResult<ContractPrefillResponse> getPrefillData(
            @RequestParam(value = "quotationId", required = false) Long quotationId,
            @RequestParam(value = "contractId", required = false) Long contractId) {
        return ApiResult.success(contractService.getPrefillData(quotationId, contractId));
    }

    @Operation(summary = "반려된 계약서 목록 조회", description = "재작성을 위해 참고할 반려된 계약서 목록을 조회합니다.")
    @GetMapping("/rejected")
    public ApiResult<List<ContractListResponse>> getRejectedContracts() {
        return ApiResult.success(contractService.getRejectedContracts());
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
