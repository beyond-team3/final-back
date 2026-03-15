package com.monsoon.seedflowplus.domain.note.dto.response;

import com.monsoon.seedflowplus.domain.note.entity.SalesBriefing;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "고객별 AI 영업 브리핑 응답 DTO")
public class BriefingResponseDto {

    @Schema(description = "브리핑 ID (PK)")
    private Long id;

    @Schema(description = "고객사 ID")
    private Long clientId;

    @Schema(description = "최근 현황 및 변화 요약")
    private List<String> statusChange;

    @Schema(description = "장기 패턴 및 특이사항")
    private List<String> longTermPattern;

    @Schema(description = "분석 근거가 된 영업 노트 ID 리스트")
    private List<Long> evidenceNoteIds;

    @Schema(description = "AI 추천 영업 전략")
    private String strategySuggestion;

    @Schema(description = "데이터 갱신 버전")
    private Integer revision;

    @Schema(description = "생성 일시")
    private LocalDateTime createdAt;

    @Schema(description = "수정 일시")
    private LocalDateTime updatedAt;

    public static BriefingResponseDto from(SalesBriefing entity) {
        return BriefingResponseDto.builder()
                .id(entity.getId())
                .clientId(entity.getClientId())
                .statusChange(entity.getStatusChange())
                .longTermPattern(entity.getLongTermPattern())
                .evidenceNoteIds(entity.getEvidenceNoteIds())
                .strategySuggestion(entity.getStrategySuggestion())
                .revision(entity.getRevision())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
