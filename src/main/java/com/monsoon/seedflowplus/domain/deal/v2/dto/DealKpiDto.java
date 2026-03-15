package com.monsoon.seedflowplus.domain.deal.v2.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DealKpiDto {

    private long dealCount;
    private long openDealCount;
    private long closedDealCount;
    private long successfulDealCount;
    private BigDecimal successRate;
    private BigDecimal averageLeadTimeDays;
    private BigDecimal quotationToContractConversionRate;
    private BigDecimal rewriteRate;
}
