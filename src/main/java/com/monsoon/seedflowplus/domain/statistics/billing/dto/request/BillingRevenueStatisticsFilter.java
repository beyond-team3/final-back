package com.monsoon.seedflowplus.domain.statistics.billing.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BillingRevenueStatisticsFilter {

    @NotNull
    private LocalDate fromDate;

    @NotNull
    private LocalDate toDate;

    private String category;
}
