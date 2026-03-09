package com.monsoon.seedflowplus.domain.billing.statement.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.billing.statement.dto.response.StatementListResponse;
import com.monsoon.seedflowplus.domain.billing.statement.dto.response.StatementResponse;
import com.monsoon.seedflowplus.domain.billing.statement.service.StatementService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Statement", description = "명세서 API")
@RestController
@RequestMapping("/api/v1/statements")
@RequiredArgsConstructor
public class StatementController {

    private final StatementService statementService;

    @Operation(summary = "명세서 단건 조회", description = "명세서 ID로 단건 조회합니다.")
    @GetMapping("/{statementId}")
    public ApiResult<StatementResponse> getStatement(
            @PathVariable Long statementId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResult.success(
                statementService.getStatement(statementId, userDetails)
        );
    }

    @Operation(summary = "명세서 목록 조회", description = "전체 명세서 목록을 조회합니다.")
    @GetMapping
    public ApiResult<List<StatementListResponse>> getStatements() {
        return ApiResult.success(statementService.getStatements());
    }

    @Operation(summary = "명세서 취소", description = "명세서 상태를 CANCELED로 변경합니다.")
    @PatchMapping("/{statementId}/cancel")
    public ApiResult<StatementResponse> cancelStatement(
            @PathVariable Long statementId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResult.success(statementService.cancelStatement(statementId, userDetails));
    }
}
