package com.monsoon.seedflowplus.domain.statistics.dto;

import java.math.BigDecimal;

public record SalesRankingDto(
        int rank,
        String targetId,
        String targetName,
        BigDecimal sales
) {
}
