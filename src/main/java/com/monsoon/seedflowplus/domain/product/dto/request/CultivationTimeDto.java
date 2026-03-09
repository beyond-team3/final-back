package com.monsoon.seedflowplus.domain.product.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "상품 재배적기(월 단위, 1~12) 요청 DTO")
public class CultivationTimeDto {

    @Schema(description = "작형 (예: 터널, 봄노지)", example = "봄노지")
    private String croppingSystem;

    @Schema(description = "지역 (예: 남부, 중부권)", example = "남부")
    private String region;

    @Schema(description = "파종 시작 월", example = "2")
    private Integer sowingStart;

    @Schema(description = "파종 종료 월", example = "4")
    private Integer sowingEnd;

    @Schema(description = "정식 시작 월", example = "4")
    private Integer plantingStart;

    @Schema(description = "정식 종료 월", example = "5")
    private Integer plantingEnd;

    @Schema(description = "수확 시작 월", example = "7")
    private Integer harvestingStart;

    @Schema(description = "수확 종료 월", example = "10")
    private Integer harvestingEnd;

    @Builder
    public CultivationTimeDto(String croppingSystem, String region, Integer sowingStart, Integer sowingEnd,
            Integer plantingStart, Integer plantingEnd,
            Integer harvestingStart, Integer harvestingEnd) {
        this.croppingSystem = croppingSystem;
        this.region = region;
        this.sowingStart = sowingStart;
        this.sowingEnd = sowingEnd;
        this.plantingStart = plantingStart;
        this.plantingEnd = plantingEnd;
        this.harvestingStart = harvestingStart;
        this.harvestingEnd = harvestingEnd;
    }
}
