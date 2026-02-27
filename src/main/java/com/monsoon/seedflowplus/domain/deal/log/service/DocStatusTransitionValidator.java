package com.monsoon.seedflowplus.domain.deal.log.service;

import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.deal.log.policy.DocStatusTransitionPolicy;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DocStatusTransitionValidator {

    public void validateOrThrow(DealType dealType, String fromStatus, ActionType actionType, String toStatus) {
        if (DocStatusTransitionPolicy.isAllowed(dealType, fromStatus, actionType, toStatus)) {
            return;
        }

        boolean terminal = DocStatusTransitionPolicy.isTerminalStatus(dealType, fromStatus);
        if (terminal) {
            throw new CoreException(
                    ErrorType.INVALID_DOC_STATUS_TRANSITION,
                    "최종 상태에서는 상태 변경을 재요청할 수 없습니다."
                            + " dealType=" + dealType
                            + ", fromStatus=" + fromStatus
                            + ", actionType=" + actionType
                            + ", toStatus=" + toStatus
            );
        }

        Set<String> allowedToStatuses = DocStatusTransitionPolicy.allowedToStatuses(dealType, fromStatus, actionType);
        throw new CoreException(
                ErrorType.INVALID_DOC_STATUS_TRANSITION,
                "허용되지 않은 상태 전이입니다."
                        + " dealType=" + dealType
                        + ", fromStatus=" + fromStatus
                        + ", actionType=" + actionType
                        + ", toStatus=" + toStatus
                        + ", allowedToStatuses=" + allowedToStatuses
        );
    }

    public boolean isAllowed(DealType dealType, String fromStatus, ActionType actionType, String toStatus) {
        return DocStatusTransitionPolicy.isAllowed(dealType, fromStatus, actionType, toStatus);
    }
}
