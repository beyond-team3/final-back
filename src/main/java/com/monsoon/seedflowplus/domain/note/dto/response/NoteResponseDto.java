package com.monsoon.seedflowplus.domain.note.dto.response;

import com.monsoon.seedflowplus.domain.note.entity.SalesNote;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "영업 활동 기록 응답 DTO")
public class NoteResponseDto {

    @Schema(description = "영업 노트 ID (PK)")
    private Long id;

    @Schema(description = "고객사 ID")
    private Long clientId;

    @Schema(description = "작성자 ID")
    private Long authorId;

    @Schema(description = "관련 계약 ID")
    private String contractId;

    @Schema(description = "활동 일자")
    private LocalDate activityDate;

    @Schema(description = "미팅/활동 내용 원문")
    private String content;

    @Schema(description = "AI 생성 요약 (리스트)")
    private List<String> aiSummary;

    @Schema(description = "수정 여부")
    private boolean isEdited;

    @Schema(description = "생성 일시")
    private LocalDateTime createdAt;

    @Schema(description = "수정 일시")
    private LocalDateTime updatedAt;

    public static NoteResponseDto from(SalesNote entity) {
        return NoteResponseDto.builder()
                .id(entity.getId())
                .clientId(entity.getClientId())
                .authorId(entity.getAuthorId())
                .contractId(entity.getContractId())
                .activityDate(entity.getActivityDate())
                .content(entity.getContent())
                .aiSummary(entity.getAiSummary())
                .isEdited(entity.isEdited())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
