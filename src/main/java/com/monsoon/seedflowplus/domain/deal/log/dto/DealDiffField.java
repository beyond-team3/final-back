package com.monsoon.seedflowplus.domain.deal.log.dto;

public record DealDiffField(
        String field,
        String label,
        Object before,
        Object after,
        String type
) {
}
