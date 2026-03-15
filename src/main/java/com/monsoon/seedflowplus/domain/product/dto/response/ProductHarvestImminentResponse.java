package com.monsoon.seedflowplus.domain.product.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductHarvestImminentResponse {

    private Integer month;
    private Integer nextMonth;
    private List<ClientHarvestImminentItem> clients;

    @Getter
    @Builder
    public static class ClientHarvestImminentItem {
        private Long clientId;
        private String clientName;
        private List<CropHarvestImminentItem> crops;
    }

    @Getter
    @Builder
    public static class CropHarvestImminentItem {
        private String cropName;
        private List<HarvestProductItem> matchedProducts;
    }

    @Getter
    @Builder
    public static class HarvestProductItem {
        private Long productId;
        private String productName;
        private String productCategory;
        private String productCategoryLabel;
        private String imageUrl;
        private Integer harvestingStart;
        private Integer harvestingEnd;
        private String croppingSystem;
        private String region;
    }
}
