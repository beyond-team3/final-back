package com.monsoon.seedflowplus.domain.sales.contract.dto.response;

import com.monsoon.seedflowplus.domain.sales.contract.entity.BillingCycle;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ContractResponse(
        Long id,
        String contractCode,
        Long quotationId,
        String clientName,
        String salesRepName,
        ContractStatus status,
        BigDecimal totalAmount,
        LocalDate startDate,
        LocalDate endDate,
        BillingCycle billingCycle,
        String specialTerms,
        String memo,
        LocalDateTime createdAt,
        List<ItemResponse> items) {
    public record ItemResponse(
            Long productId,
            String productName,
            String productCategory,
            Integer totalQuantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal amount) {
    }
}
