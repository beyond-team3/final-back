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

    @Lob
    @Column(columnDefinition = "TEXT")
    private String strategySuggestion; // AI 추천 전략

    @Column(length = 20)
    private String version; // 브리핑 버전

    @Builder
    public SalesBriefing(Long clientId, List<String> statusChange, List<String> longTermPattern, String strategySuggestion, String version) {
        this.clientId = clientId;
        this.statusChange = statusChange;
        this.longTermPattern = longTermPattern;
        this.strategySuggestion = strategySuggestion;
        this.version = version;
    }

    /**
     * [비즈니스 로직] 기존 브리핑 데이터 업데이트
     * AI 재분석 결과가 나왔을 때 기존 엔티티의 상태를 변경합니다.
     */
    public void updateAnalysis(List<String> statusChange, List<String> longTermPattern, String strategySuggestion, String version) {
        this.statusChange = statusChange;
        this.longTermPattern = longTermPattern;
        this.strategySuggestion = strategySuggestion;
        this.version = version;

        // 참고: BaseModifyEntity를 상속받았으므로,
        // 영속성 컨텍스트에 의해 Dirty Checking이 발생하면 updatedAt은 자동으로 갱신됩니다.
    }
}
