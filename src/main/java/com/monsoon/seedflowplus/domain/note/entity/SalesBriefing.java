package com.monsoon.seedflowplus.domain.note.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "tbl_sales_briefing")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalesBriefing extends BaseModifyEntity {

    @Column(nullable = false, unique = true)
    private Long clientId; // 고객사별 1:1 매칭

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "status_change", columnDefinition = "json")
    private List<String> statusChange; // 핵심 현황 및 최근 변화 리스트

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "long_term_pattern", columnDefinition = "json")
    private List<String> longTermPattern; // 장기 패턴 및 특이사항 리스트

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_note_ids", columnDefinition = "json")
    private List<Long> evidenceNoteIds; // [추가] 분석 근거가 된 노트 ID 리스트

    @Lob
    @Column(columnDefinition = "TEXT")
    private String strategySuggestion; // AI 추천 전략

    @Column(length = 20)
    private String version; // 브리핑 버전

    @Builder
    public SalesBriefing(Long clientId, List<String> statusChange, List<String> longTermPattern,
                         List<Long> evidenceNoteIds, String strategySuggestion, String version) {
        this.clientId = clientId;
        this.statusChange = statusChange;
        this.longTermPattern = longTermPattern;
        this.evidenceNoteIds = evidenceNoteIds;
        this.strategySuggestion = strategySuggestion;
        this.version = version;
    }

    /**
     * [비즈니스 로직] 기존 브리핑 데이터 업데이트
     * 근거 추출(Citation) 데이터가 추가되었습니다.
     */
    public void updateAnalysis(List<String> statusChange, List<String> longTermPattern,
                               List<Long> evidenceNoteIds, String strategySuggestion, String version) {
        // 불변 리스트 복사 및 Null 방어
        this.statusChange = (statusChange == null) ? List.of() : List.copyOf(statusChange);
        this.longTermPattern = (longTermPattern == null) ? List.of() : List.copyOf(longTermPattern);
        this.evidenceNoteIds = (evidenceNoteIds == null) ? List.of() : List.copyOf(evidenceNoteIds);

        this.strategySuggestion = (strategySuggestion == null) ? "" : strategySuggestion;
        this.version = version;
    }
}