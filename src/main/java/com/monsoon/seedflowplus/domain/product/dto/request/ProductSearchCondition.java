package com.monsoon.seedflowplus.domain.product.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "상품 목록 다중 조건 검색 파라미터")
public class ProductSearchCondition {

    @Schema(description = "상품 카테고리 (예: WATERMELON)", example = "WATERMELON")
    private String category;

    @Schema(description = "키워드 (상품명 또는 설명)", example = "씨앗")
    private String keyword;

    @Schema(description = "조회 대상 파종 월 (1-12)", example = "3")
    private Integer sowingMonth;

    @Schema(description = "조회 대상 정식 월 (1-12)", example = "4")
    private Integer plantingMonth;

    @Schema(description = "조회 대상 수확 월 (1-12)", example = "7")
    private Integer harvestingMonth;

}
