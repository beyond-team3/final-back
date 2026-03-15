package com.monsoon.seedflowplus.domain.product.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class SimilarProductResponse {

    private Long productId;
    private List<SimilarProductItem> similarProducts;

    @Getter
    @Builder
    public static class SimilarProductItem {
        private Long productId;
        private String productName;
        private String category;
        private Map<String, List<String>> tags;
        private int similarityScore;
    }
}