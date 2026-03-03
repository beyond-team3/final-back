package com.monsoon.seedflowplus.domain.note.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "tbl_sales_note")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Schema(description = "영업 활동 기록 (노트) 엔티티")
public class SalesNote extends BaseModifyEntity {

    @Schema(description = "고객사 ID", example = "10")
    @Column(nullable = false)
    private Long clientId; // 향후 Client 엔티티와 @ManyToOne 연계 가능

    @Schema(description = "작성자 ID", example = "1")
    @Column(nullable = false)
    private Long authorId; // 작성자 ID (기본값 1)

    @Schema(description = "관련 계약 ID", example = "CONT-2024-001")
    private String contractId; // 계약 정보

    @Schema(description = "활동 일자", example = "2024-03-03")
    @Column(nullable = false)
    private LocalDate activityDate; // 활동 일자

    @Schema(description = "미팅/활동 내용 원문", example = "신규 제품 도입에 대한 긍정적인 피드백을 받음.")
    @Lob //대용량 데이터를 저장할 때 쓰는 어노테이션
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 미팅 내용 원문

    @Schema(description = "AI 생성 요약 (리스트)", example = "[\"고객사가 신규 제품에 긍정적임\", \"예산 확보 단계 진행 중\", \"다음 주 기술 미팅 예정\"]")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_summary", columnDefinition = "json")
    private List<String> aiSummary; // AI 생성 요약 (3문장 리스트)

    @Schema(description = "수정 여부", example = "false")
    @Column(nullable = false)
    private boolean isEdited = false; // 수정 여부

    @Builder
    public SalesNote(Long clientId, Long authorId, String contractId, LocalDate activityDate, String content, List<String> aiSummary) {
        this.clientId = clientId;
        this.authorId = authorId;
        this.contractId = contractId;
        this.activityDate = activityDate;
        this.content = content;
        this.aiSummary = aiSummary;
    }

    // 비즈니스 메서드: 데이터 수정 시 호출
    public void updateNote(String content, String contractId, LocalDate activityDate, List<String> newSummary) {
        this.content = content;
        this.contractId = contractId;
        this.activityDate = activityDate;
        this.aiSummary = newSummary;
        this.isEdited = true; // 수정 상태로 변경
    }
}