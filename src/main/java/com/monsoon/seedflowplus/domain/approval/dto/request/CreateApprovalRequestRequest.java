package com.monsoon.seedflowplus.domain.approval.dto.request;

import com.monsoon.seedflowplus.domain.deal.common.DealType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateApprovalRequestRequest(
        @NotNull DealType dealType,
        @NotNull @Positive Long targetId,
        Long clientIdSnapshot,
        String targetCodeSnapshot
) {
}
