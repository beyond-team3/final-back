package com.monsoon.seedflowplus.domain.note.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.note.dto.response.RagSeedResponseDto;
import com.monsoon.seedflowplus.domain.note.service.RagSeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "RAGseed Engine", description = "RAGseed: 영업 데이터(seed)에서 추출한 전략 인출 엔진")
@RestController
@RequestMapping("/api/v1/ragseed")
@RequiredArgsConstructor
public class RagSeedController {

    private final RagSeedService ragSeedService;

    @Operation(summary = "RAGseed 전략 인출", 
               description = "특정 고객 또는 계약에 대해 맞춤형 전략을 인출합니다. query 파라미터에 'RECAP', 'RISK', 'MATCHING', 'CHECKLIST' 등을 사용하거나 일반 질문을 입력하세요.")
    @GetMapping("/ask")
    public ApiResult<RagSeedResponseDto> askRagSeed(
            @Parameter(description = "고객사 ID (전사 분석 시 생략)") @RequestParam(required = false) Long clientId,
            @Parameter(description = "계약 코드 (고객별 분석 시 생략 또는 'NONE')") @RequestParam(required = false) String contractId,
            @Parameter(description = "인출 쿼리 또는 템플릿 타입(RECAP, RISK, MATCHING, CHECKLIST)") @RequestParam String query) {
        
        return ApiResult.success(ragSeedService.getTargetedStrategy(clientId, contractId, query));
    }
}
