package com.monsoon.seedflowplus.domain.deal.log.service;

import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.log.entity.SalesDealLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DealPipelineFacade {

    private final DealLogWriteService dealLogWriteService;
    private final DocStatusTransitionValidator docStatusTransitionValidator;

    @Transactional
    public SalesDealLog recordAndSync(
            SalesDeal deal,
            DealType docType,
            Long refId,
            String targetCode,
            DealStage fromStage,
            DealStage toStage,
            String fromStatus,
            String toStatus,
            ActionType actionType,
            java.time.LocalDateTime actionAt,
            ActorType actorType,
            Long actorId,
            String reason,
            java.util.List<DealLogWriteService.DiffField> diffFields
    ) {
        validateTransitionIfNeeded(docType, fromStatus, toStatus, actionType);

        SalesDealLog savedLog = dealLogWriteService.write(
                deal,
                docType,
                refId,
                targetCode,
                fromStage,
                toStage,
                fromStatus,
                toStatus,
                actionType,
                actionAt,
                actorType,
                actorId,
                reason,
                diffFields
        );

        deal.updateSnapshot(
                toStage,
                toStatus,
                docType,
                refId,
                targetCode,
                savedLog.getActionAt()
        );
        return savedLog;
    }

    @Transactional
    public DealLogWriteService.ConvertLogPair recordConvertAndSync(
            DealLogWriteService.ConvertLogRequest original,
            DealLogWriteService.ConvertLogRequest created
    ) {
        validateTransitionIfNeeded(
                original.docType(),
                original.fromStatus(),
                original.toStatus(),
                ActionType.CONVERT
        );

        DealLogWriteService.ConvertLogPair pair = dealLogWriteService.writeConvertPair(original, created);
        created.deal().updateSnapshot(
                DealStage.CREATED,
                created.toStatus(),
                created.docType(),
                created.refId(),
                created.targetCode(),
                pair.createdLog().getActionAt()
        );
        return pair;
    }

    public void validateTransitionOrThrow(DealType docType, String fromStatus, ActionType actionType, String toStatus) {
        docStatusTransitionValidator.validateOrThrow(docType, fromStatus, actionType, toStatus);
    }

    private void validateTransitionIfNeeded(DealType docType, String fromStatus, String toStatus, ActionType actionType) {
        if (!shouldValidateTransition(actionType, fromStatus, toStatus)) {
            return;
        }
        docStatusTransitionValidator.validateOrThrow(docType, fromStatus, actionType, toStatus);
    }

    private boolean shouldValidateTransition(ActionType actionType, String fromStatus, String toStatus) {
        if (!StringUtils.hasText(fromStatus) || !StringUtils.hasText(toStatus)) {
            return false;
        }
        if (fromStatus.equals(toStatus)) {
            return false;
        }
        return actionType != ActionType.CREATE && actionType != ActionType.UPDATE;
    }
}
