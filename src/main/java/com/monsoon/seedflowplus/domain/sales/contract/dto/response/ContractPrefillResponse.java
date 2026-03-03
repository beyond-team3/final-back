package com.monsoon.seedflowplus.domain.sales.contract.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ContractPrefillResponse(
        Long quotationId,
        String quotationCode,
        Long clientId,
        String clientName,
        String representativeName,
        BigDecimal totalAmount,
        List<Item> items) {
    public record Item(
            Long productId,
            String productName,
            String productCategory,
            Integer totalQuantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal amount) {
    }
}
