package com.monsoon.seedflowplus.domain.note.dto;

import com.monsoon.seedflowplus.domain.note.entity.SalesNote;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "영업 활동 기록 저장/수정 요청 DTO")
public class NoteRequestDto {

    @Schema(description = "고객사 ID", example = "10")
    private Long clientId;

    @Schema(description = "관련 계약 ID (선택)", example = "CONT-2024-001")
    private String contractId;

    @Schema(description = "활동 일자", example = "2024-03-03")
    private LocalDate date;     // 내부 필드명은 date 유지 (JSON 바인딩용)

    @Schema(description = "미팅/활동 내용 원문", example = "신규 제품 도입에 대한 긍정적인 피드백을 받음.")
    private String content;

    @Schema(description = "AI 요약 내용 (3문장 권장)", example = "[\"고객사가 신규 제품에 긍정적임\", \"예산 확보 단계 진행 중\", \"다음 주 기술 미팅 예정\"]")
    private List<String> aiSummary;

    /**
     * 서비스 로직(updateNote)에서 요구하는 날짜 반환 메서드
     *
     */
    public LocalDate getActivityDate() {
        return this.date;
    }

    /**
     * DTO를 SalesNote 엔티티로 변환합니다.
     */
    public SalesNote toEntity(Long currentUserId) { // authorId를 파라미터로 받음
        return SalesNote.builder()
                .clientId(this.clientId)
                .authorId(currentUserId) // 주입받은 ID 매핑
                .contractId(this.contractId)
                .activityDate(this.date)
                .content(this.content)
                .aiSummary(this.aiSummary)
                .build();
    }
}