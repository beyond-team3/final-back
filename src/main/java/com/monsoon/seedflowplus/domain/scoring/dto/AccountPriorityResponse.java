package com.monsoon.seedflowplus.domain.scoring.dto;

import lombok.Builder;

@Builder
public record AccountPriorityResponse(
        Long accountId,
        String accountName,
        double totalScore,
        String primaryReason,
        String detailDescription,
        ScoreBreakdown breakdown
) {
    public record ScoreBreakdown(
            double contractScore,
            double orderScore,
            double visitScore
    ) {}
}
