package com.monsoon.seedflowplus.domain.approval.dto.response;

import com.monsoon.seedflowplus.domain.approval.entity.ApprovalStepStatus;
import com.monsoon.seedflowplus.domain.approval.entity.DecisionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import java.time.LocalDateTime;

public record ApprovalStepResponse(
        Long stepId,
        int stepOrder,
        ActorType actorType,
        ApprovalStepStatus status,
        LocalDateTime decidedAt,
        DecisionType decision,
        String reason,
        Long decidedByUserId
) {
}
