package com.monsoon.seedflowplus.domain.approval.dto.request;

import com.monsoon.seedflowplus.domain.approval.entity.DecisionType;
import jakarta.validation.constraints.NotNull;

public record DecideApprovalRequest(
        @NotNull DecisionType decision,
        String reason
) {
}
