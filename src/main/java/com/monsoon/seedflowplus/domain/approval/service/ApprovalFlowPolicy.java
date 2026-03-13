package com.monsoon.seedflowplus.domain.approval.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalStep;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalStepStatus;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ApprovalFlowPolicy {

    public void validateSupportedDealType(DealType dealType) {
        if (dealType != DealType.QUO && dealType != DealType.CNT && dealType != DealType.ORD) {
            throw new CoreException(ErrorType.APPROVAL_UNSUPPORTED_DEAL_TYPE);
        }
    }

    public List<ApprovalStep> createSteps(DealType dealType) {
        return switch (dealType) {
            case QUO, CNT -> List.of(
                    step(1, ActorType.ADMIN),
                    step(2, ActorType.CLIENT)
            );
            case ORD -> List.of(step(1, ActorType.SALES_REP));
            default -> throw new CoreException(ErrorType.APPROVAL_UNSUPPORTED_DEAL_TYPE);
        };
    }

    public boolean isLastStep(DealType dealType, int stepOrder) {
        return stepOrder == lastStepOrder(dealType);
    }

    private int lastStepOrder(DealType dealType) {
        return switch (dealType) {
            case QUO, CNT -> 2;
            case ORD -> 1;
            default -> throw new CoreException(ErrorType.APPROVAL_UNSUPPORTED_DEAL_TYPE);
        };
    }

    private ApprovalStep step(int stepOrder, ActorType actorType) {
        return ApprovalStep.builder()
                .stepOrder(stepOrder)
                .actorType(actorType)
                .status(ApprovalStepStatus.WAITING)
                .build();
    }
}
