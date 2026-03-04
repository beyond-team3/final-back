package com.monsoon.seedflowplus.domain.statistics.billing.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BillingRevenueStatisticsFilter {

    @NotNull
    private LocalDate fromDate;

    @NotNull
    private LocalDate toDate;

    private String category;
}
