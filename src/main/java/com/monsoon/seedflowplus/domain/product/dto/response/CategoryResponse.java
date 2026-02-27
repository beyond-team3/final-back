package com.monsoon.seedflowplus.domain.product.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "상품 카테고리 정보 DTO")
public class CategoryResponse {

    @Schema(description = "카테고리 영문 코드", example = "WATERMELON")
    private String code;

    @Schema(description = "카테고리 한글 명칭", example = "수박")
    private String name;

}
