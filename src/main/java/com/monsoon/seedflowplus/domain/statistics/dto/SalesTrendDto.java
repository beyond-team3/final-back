package com.monsoon.seedflowplus.domain.statistics.dto;

import java.util.List;

public record SalesTrendDto(
        String targetId,
        String targetName,
        List<SalesTrendItemDto> data
) {
}
