package com.monsoon.seedflowplus.domain.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.monsoon.seedflowplus.domain.product.dto.request.CultivationTimeDto;

@Getter
@Builder
public class ProductResponse {

    private Long id;
    private String category;
    private String name;
    private String description;
    private String imageUrl;
    private PriceData priceData;
    private Map<String, List<String>> tags;
    private CultivationTimeDto cultivationTime;

    @Getter
    @AllArgsConstructor
    public static class PriceData {
        private Integer amount; // 수량
        private BigDecimal price; // 단가
        private String unit; // 단위
    }
}
