package com.monsoon.seedflowplus.domain.sales.contract.dto.response;

import com.monsoon.seedflowplus.domain.sales.contract.entity.BillingCycle;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.deal.log.dto.response.DealLogSummaryDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ContractResponse(
        Long headerId, // reason: 계약 헤더 PK임을 detailId와 명확히 구분하기 위해 필드명을 headerId로 변경
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
