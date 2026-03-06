package com.monsoon.seedflowplus.domain.note.dto.request;

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

    @Schema(description = "고객사 ID")
    private Long clientId;

    @Schema(description = "관련 계약 ID (선택)")
    private String contractId;

    @Schema(description = "활동 일자")
    private LocalDate date;     // 내부 필드명은 date 유지 (JSON 바인딩용)

    @Schema(description = "미팅/활동 내용 원문")
    private String content;

    /**
     * 서비스 로직(updateNote)에서 요구하는 날짜 반환 메서드
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
                .aiSummary(null) // 서비스에서 채워질 예정
                .build();
    }
}
