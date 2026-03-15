package com.monsoon.seedflowplus.domain.approval.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.approval.dto.request.CreateApprovalRequestRequest;
import com.monsoon.seedflowplus.domain.approval.dto.request.DecideApprovalRequest;
import com.monsoon.seedflowplus.domain.approval.dto.response.ApprovalDetailResponse;
import com.monsoon.seedflowplus.domain.approval.dto.response.CreateApprovalRequestResponse;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalStatus;
import com.monsoon.seedflowplus.domain.approval.service.ApprovalCommandService;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Approval", description = "Approval API")
@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalCommandService approvalCommandService;

    @Operation(summary = "승인 요청 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResult<CreateApprovalRequestResponse> createApprovalRequest(
            @RequestBody @Valid CreateApprovalRequestRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResult.success(approvalCommandService.createApprovalRequest(request, userDetails));
    }

    @Operation(summary = "승인/반려 결정")
    @PostMapping("/{approvalId}/steps/{stepId}/decision")
    public ApiResult<ApprovalDetailResponse> decideStep(
            @PathVariable Long approvalId,
            @PathVariable Long stepId,
            @RequestBody @Valid DecideApprovalRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResult.success(approvalCommandService.decideStep(approvalId, stepId, request, userDetails));
    }

    @Operation(summary = "승인 요청 상세 조회")
    @GetMapping("/{approvalId}")
    public ApiResult<ApprovalDetailResponse> getApproval(
            @PathVariable Long approvalId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResult.success(approvalCommandService.getApproval(approvalId, userDetails));
    }

    @Operation(summary = "승인 요청 검색")
    @GetMapping
    public ApiResult<Page<ApprovalDetailResponse>> search(
            @RequestParam(required = false) ApprovalStatus status,
            @RequestParam(required = false) DealType dealType,
            @RequestParam(required = false) Long targetId,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResult.success(approvalCommandService.search(status, dealType, targetId, pageable, userDetails));
    }
}
