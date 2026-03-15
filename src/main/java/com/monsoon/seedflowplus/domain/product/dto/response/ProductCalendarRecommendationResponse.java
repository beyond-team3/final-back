package com.monsoon.seedflowplus.domain.product.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductCalendarRecommendationResponse {

    private Integer month;
    private List<RecommendedProductItem> items;

    @Getter
    @Builder
    public static class RecommendedProductItem {
        private Long productId;
        private String productName;
        private String productCategory;
        private String productCategoryLabel;
        private String description;
        private String imageUrl;
        private Integer sowingStart;
        private Integer plantingStart;
        private String croppingSystem;
        private String region;
    }
}
