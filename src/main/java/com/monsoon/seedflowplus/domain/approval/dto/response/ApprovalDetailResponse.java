package com.monsoon.seedflowplus.domain.approval.dto.response;

import com.monsoon.seedflowplus.domain.approval.entity.ApprovalStatus;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import java.util.List;

public record ApprovalDetailResponse(
        Long approvalId,
        DealType dealType,
        Long targetId,
        ApprovalStatus status,
        Long clientIdSnapshot,
        String targetCodeSnapshot,
        List<ApprovalStepResponse> steps
) {
}
