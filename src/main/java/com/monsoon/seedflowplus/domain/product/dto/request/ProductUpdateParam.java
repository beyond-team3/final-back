package com.monsoon.seedflowplus.domain.product.dto.request;

import com.monsoon.seedflowplus.core.common.validator.ValidEnum;
import com.monsoon.seedflowplus.domain.product.entity.ProductCategory;
import com.monsoon.seedflowplus.domain.product.entity.ProductStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductUpdateParam(
        @NotBlank(message = "상품명은 필수입니다.") String productName,

        @NotBlank(message = "카테고리는 필수입니다.") @ValidEnum(enumClass = ProductCategory.class, message = "유효하지 않은 카테고리입니다.") String productCategory,

        String productDescription,

        String productImageUrl,

        @NotNull(message = "수량은 필수입니다.") @Min(value = 0, message = "수량은 0 이상이어야 합니다.") Integer amount,

        @NotBlank(message = "단위는 필수입니다.") String unit,

        @NotNull(message = "단가는 필수입니다.") @Min(value = 0, message = "단가는 0원 이상이어야 합니다.") BigDecimal price,

        @NotBlank(message = "상태값은 필수입니다.") @ValidEnum(enumClass = ProductStatus.class, message = "유효하지 않은 상태입니다.") String status,

        Map<String, List<String>> tags,

        List<CultivationTimeDto> cultivationTimes) {
}
