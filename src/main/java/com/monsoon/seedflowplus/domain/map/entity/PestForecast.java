package com.monsoon.seedflowplus.domain.map.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tbl_pest_forecast")
public class PestForecast {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String areaName;     // 시군구명 (예: 청양군)

    @Column(nullable = false)
    private String cropCode;     // 작물 코드 (예: pepper)

    @Column(nullable = false)
    private String pestCode;     // 병해충 코드 (예: PP01)

    @Column(nullable = false)
    private String severity;     // 심각도 (심각, 경고, 주의, 보통)

    @Builder
    public PestForecast(String areaName, String cropCode, String pestCode, String severity) {
        this.areaName = areaName;
        this.cropCode = cropCode;
        this.pestCode = pestCode;
        this.severity = severity;
    }
}