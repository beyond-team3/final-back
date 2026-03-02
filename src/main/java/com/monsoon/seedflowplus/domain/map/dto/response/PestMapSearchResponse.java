package com.monsoon.seedflowplus.domain.map.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class PestMapSearchResponse {
    private List<ForecastDto> forecasts;
    private List<ProductDto> recommendedProducts;

    @Getter
    @Builder
    public static class ForecastDto {
        private String areaName;
        private String severity;
    }

    @Getter
    @Builder
    public static class ProductDto {
        private String name;
        private String description;
        private String resistance;
        private Boolean isFavorite;
    }
}