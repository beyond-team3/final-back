package com.monsoon.seedflowplus.domain.sales.contract.dto.response;

import com.monsoon.seedflowplus.domain.sales.contract.entity.BillingCycle;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.deal.log.dto.response.DealLogSummaryDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ContractResponse(
        Long id,
        String contractCode,
        Long quotationId,
        Long clientId,
        String clientName,
        Long authorId,
        String salesRepName, // authorName 역할을 함
        ContractStatus status,
        BigDecimal totalAmount,
        LocalDate startDate,
        LocalDate endDate,
        BillingCycle billingCycle,
        String specialTerms,
        String memo,
        LocalDateTime createdAt,
        List<ItemResponse> items,
        List<DealLogSummaryDto> recentLogs) {
    public record ItemResponse(
            Long detailId, // reason: 주문 생성 시 참조하는 계약 상세 PK를 응답에 노출하기 위해 추가
            Long productId,
            String productName,
            String productCategory,
            Integer totalQuantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal amount) {
    }
}
