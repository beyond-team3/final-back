package com.monsoon.seedflowplus.domain.approval.service;

import com.monsoon.seedflowplus.domain.approval.entity.DecisionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import java.time.LocalDateTime;
import java.util.Objects;

public record ContractApprovalSchedulesSyncEvent(
        Long targetId,
        DealType dealType,
        Integer stepOrder,
        ActorType actorType,
        DecisionType decision,
        LocalDateTime occurredAt,
        Long principalUserId
) {
    public ContractApprovalSchedulesSyncEvent {
        Objects.requireNonNull(targetId, "targetId must not be null");
        Objects.requireNonNull(dealType, "dealType must not be null");
        Objects.requireNonNull(stepOrder, "stepOrder must not be null");
        Objects.requireNonNull(actorType, "actorType must not be null");
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
