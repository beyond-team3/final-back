package com.monsoon.seedflowplus.domain.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class ProductRequest {

    @NotBlank(message = "상품명은 필수입니다.")
    @JsonProperty("name")
    private String productName;

    @NotBlank(message = "카테고리는 필수입니다.")
    @JsonProperty("category")
    private String productCategory;

    @JsonProperty("desc")
    private String productDescription;

    @JsonProperty("imageUrl")
    private String productImageUrl;

    @NotNull(message = "수량은 필수입니다.")
    private Integer amount;

    @NotBlank(message = "단위는 필수입니다.")
    private String unit;

    @NotNull(message = "단가는 필수입니다.")
    private BigDecimal price;

    @NotBlank(message = "상품 상태는 필수입니다.")
    private String status;

    private Map<String, List<String>> tags;

    @Schema(description = "재배적기 정보 목록")
    private List<CultivationTimeDto> cultivationTimes;
}
