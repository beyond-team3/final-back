package com.monsoon.seedflowplus.domain.note.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.note.dto.response.BriefingResponseDto;
import com.monsoon.seedflowplus.domain.note.service.RagSeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Sales Briefing", description = "AI 영업 브리핑 (표준 리포트)")
@RestController
@RequestMapping("/api/v1/briefing")
@RequiredArgsConstructor
public class BriefingController {

    private final RagSeedService ragSeedService;

    @Operation(summary = "고객별 표준 AI 브리핑 조회", description = "고객사의 최근 영업 데이터를 요약한 표준 브리핑 리포트를 조회합니다.")
    @GetMapping("/{clientId}")
    public ApiResult<BriefingResponseDto> getBriefing(@PathVariable Long clientId) {
        return ragSeedService.getBriefingByClient(clientId)
                .map(entity -> ApiResult.success(BriefingResponseDto.from(entity)))
                .orElse(ApiResult.success(null)); // 데이터가 없는 경우 null 반환
    }
}
