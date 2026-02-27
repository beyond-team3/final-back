package com.monsoon.seedflowplus.domain.note.dto;

import com.monsoon.seedflowplus.domain.note.entity.SalesNote;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteRequestDto {

    private Long clientId;
    private String contractId;
    private LocalDate date;     // 내부 필드명은 date 유지 (JSON 바인딩용)
    private String content;
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