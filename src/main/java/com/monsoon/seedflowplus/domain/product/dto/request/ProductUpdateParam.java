package com.monsoon.seedflowplus.domain.product.dto.request;

import com.monsoon.seedflowplus.domain.product.entity.ProductCategory;
import com.monsoon.seedflowplus.domain.product.entity.ProductStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductUpdateParam(
        String productName,
        ProductCategory productCategory,
        String productDescription,
        String productImageUrl,
        Integer amount,
        String unit,
        BigDecimal price,
        ProductStatus status,
        Map<String, List<String>> tags
        ) {}
