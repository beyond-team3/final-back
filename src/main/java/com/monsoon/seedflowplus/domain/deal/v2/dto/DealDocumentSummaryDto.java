package com.monsoon.seedflowplus.domain.deal.v2.dto;

import com.monsoon.seedflowplus.domain.deal.common.DealType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DealDocumentSummaryDto {

    private String surrogateId;
    private DealType documentType;
    private Long documentId;
    private String documentCode;
    private BigDecimal amount;
    private LocalDate expiredDate;
    private String currentStatus;
    private LocalDateTime createdAt;
}
