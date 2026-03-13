package com.monsoon.seedflowplus.domain.note.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * RAGseed 통합 전략 엔진의 분석 결과 공통 DTO입니다.
 */
@Getter
@Builder
@Schema(description = "RAGseed 분석 결과 공통 응답")
public class RagSeedResponseDto {

    @Schema(description = "분석된 핵심 콘텐츠 (마크다운 형식 포함 가능)")
    private String content;

    @Schema(description = "분석의 근거가 된 데이터 ID 리스트 (Note ID 또는 Product ID)")
    private List<Long> evidenceIds;

    @Schema(description = "데이터 출처 및 브랜드 문구")
    private String attribution;
}
