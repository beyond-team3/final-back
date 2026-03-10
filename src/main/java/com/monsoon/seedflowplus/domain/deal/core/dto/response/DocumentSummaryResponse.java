package com.monsoon.seedflowplus.domain.deal.core.dto.response;

import com.monsoon.seedflowplus.domain.deal.common.DealType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DocumentSummaryResponse(
        String surrogateId,
        DealType docType,
        Long docId,
        String docCode,
        BigDecimal amount,
        LocalDate expiredDate,
        String status,
        LocalDateTime createdAt,
        String clientName,
        String ownerEmployeeName
) {
}
