package com.monsoon.seedflowplus.domain.approval.dto.response;

import com.monsoon.seedflowplus.domain.approval.entity.ApprovalStatus;
import com.monsoon.seedflowplus.domain.deal.common.DealType;

public record CreateApprovalRequestResponse(
        Long approvalId,
        DealType dealType,
        Long targetId,
        ApprovalStatus status
) {
}
