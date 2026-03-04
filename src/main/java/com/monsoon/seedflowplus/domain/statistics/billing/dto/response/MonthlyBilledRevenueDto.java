package com.monsoon.seedflowplus.domain.statistics.billing.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MonthlyBilledRevenueDto {
    private String month;
    private BigDecimal billedRevenue;
}
