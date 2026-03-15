package com.monsoon.seedflowplus.domain.note.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "tbl_sales_briefing")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "고객별 AI 영업 브리핑 및 전략 리포트 엔티티")
public class SalesBriefing extends BaseModifyEntity {

    @Schema(description = "고객사 ID")
    @Column(nullable = false, unique = true)
    private Long clientId; // 고객사별 1:1 매칭

    @Schema(description = "최근 현황 및 변화 요약")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "status_change", columnDefinition = "json")
    private List<String> statusChange; // 핵심 현황 및 최근 변화 리스트

    @Schema(description = "장기 패턴 및 특이사항")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "long_term_pattern", columnDefinition = "json")
    private List<String> longTermPattern; // 장기 패턴 및 특이사항 리스트

    @Schema(description = "분석 근거가 된 영업 노트 ID 리스트")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_note_ids", columnDefinition = "json")
    private List<Long> evidenceNoteIds; // [추가] 분석 근거가 된 노트 ID 리스트

    @Schema(description = "AI 추천 영업 전략")
    @Lob
    @Column(columnDefinition = "TEXT")
    private String strategySuggestion; // AI 추천 전략

    @Schema(description = "데이터 갱신 버전 (수정 시마다 1씩 증가)")
    @Column(nullable = false)
    private Integer revision; // 데이터 갱신 버전

    @Builder
    public SalesBriefing(Long clientId, List<String> statusChange, List<String> longTermPattern,
                         List<Long> evidenceNoteIds, String strategySuggestion) {
        this.clientId = clientId;
        this.statusChange = statusChange;
        this.longTermPattern = longTermPattern;
        this.evidenceNoteIds = evidenceNoteIds;
        this.strategySuggestion = strategySuggestion;
        this.revision = 1; // 신규 생성 시 1로 시작
    }

    /**
     * [비즈니스 로직] 기존 브리핑 데이터 업데이트
     * 데이터가 갱신될 때마다 revision 숫자를 올립니다.
     */
    public void updateAnalysis(List<String> statusChange, List<String> longTermPattern,
                               List<Long> evidenceNoteIds, String strategySuggestion) {
        // 불변 리스트 복사 및 Null 방어
        this.statusChange = (statusChange == null) ? List.of() : List.copyOf(statusChange);
        this.longTermPattern = (longTermPattern == null) ? List.of() : List.copyOf(longTermPattern);
        this.evidenceNoteIds = (evidenceNoteIds == null) ? List.of() : List.copyOf(evidenceNoteIds);

        this.strategySuggestion = (strategySuggestion == null) ? "" : strategySuggestion;
        
        if (this.revision == null) this.revision = 1;
        this.revision++; // 갱신 시 버전 증가
    }
}