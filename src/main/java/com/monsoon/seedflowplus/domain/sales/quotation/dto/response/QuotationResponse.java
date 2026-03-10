package com.monsoon.seedflowplus.domain.sales.quotation.dto.response;

import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.deal.log.dto.response.DealLogSummaryDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record QuotationResponse(
        Long id,
        String quotationCode,
        Long requestId,
        Long clientId,
        String clientName,
        Long authorId,
        String authorName,
        QuotationStatus status,
        BigDecimal totalAmount,
        LocalDate expiredDate,
        String memo,
        LocalDateTime createdAt,
        List<QuotationItemResponse> items,
        List<DealLogSummaryDto> recentLogs) {
    public record QuotationItemResponse(
            Long productId,
            String productName,
            String productCategory,
            Integer quantity,
            String unit,
            BigDecimal unitPrice,
            BigDecimal amount) {
    }
}
