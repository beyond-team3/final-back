package com.monsoon.seedflowplus.domain.statistics.dto;

import java.math.BigDecimal;

public record SalesTrendItemDto(
        String period,
        BigDecimal sales
) {
}
