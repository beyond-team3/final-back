package com.monsoon.seedflowplus.domain.approval.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalRequest;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalStep;
import com.monsoon.seedflowplus.domain.approval.entity.DecisionType;
import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.log.entity.SalesDealLog;
import com.monsoon.seedflowplus.domain.deal.log.repository.SalesDealLogRepository;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogWriteService;
import com.monsoon.seedflowplus.domain.deal.log.service.DealPipelineFacade;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApprovalDealLogWriter {

    private final SalesDealLogRepository salesDealLogRepository;
    private final DealPipelineFacade dealPipelineFacade;

    public void writeSubmit(ApprovalRequest request, ActorType actorType, Long actorId) {
        SubmitLogContext context = buildSubmitContext(request);
        dealPipelineFacade.recordAndSync(
                context.deal(),
                context.docType(),
                context.refId(),
                context.targetCode(),
                context.fromStage(),
                context.toStage(),
                context.fromStatus(),
                context.toStatus(),
                ActionType.SUBMIT,
                null,
                actorType,
                actorId,
                null,
                List.of(
                        diffField("approvalRequestId", "Approval Request ID", null, request.getId(), "REFERENCE"),
                        diffField("event", "Approval Event", null, "SUBMIT", "ACTION")
                )
        );
    }

    public void writeDecision(
            ApprovalRequest request,
            ApprovalStep step,
            DecisionType decision,
            String fromStatus,
            String toStatus,
            String fromStage,
            String toStage,
            String reason,
            ActorType actorType,
            Long actorId
    ) {
        dealPipelineFacade.recordAndSync(
                resolveDeal(request),
                request.getDealType(),
                request.getTargetId(),
                request.getTargetCodeSnapshot(),
                DealStage.valueOf(fromStage),
                DealStage.valueOf(toStage),
                fromStatus,
                toStatus,
                toActionType(decision),
                null,
                actorType,
                actorId,
                reason,
                List.of(
                        diffField("status", "문서 상태", fromStatus, toStatus, "STATUS"),
                        diffField("approvalRequestId", "Approval Request ID", null, request.getId(), "REFERENCE"),
                        diffField("stepOrder", "Approval Step Order", null, step.getStepOrder(), "REFERENCE"),
                        diffField("decision", "Approval Decision", null, decision.name(), "ACTION")
                )
        );
    }

    private SubmitLogContext buildSubmitContext(ApprovalRequest request) {
        SalesDeal deal = resolveDeal(request);
        return new SubmitLogContext(
                deal,
                request.getDealType(),
                request.getTargetId(),
                request.getTargetCodeSnapshot(),
                deal.getCurrentStage(),
                DealStage.PENDING_ADMIN,
                waitingAdminStatus(request),
                waitingAdminStatus(request)
        );
    }

    private SalesDeal resolveDeal(ApprovalRequest request) {
        SalesDealLog latestLog = salesDealLogRepository
                .findTopByDocTypeAndRefIdOrderByActionAtDescIdDesc(request.getDealType(), request.getTargetId())
                .orElseThrow(() -> new CoreException(
                        ErrorType.DEAL_NOT_FOUND,
                        "deal을 찾을 수 없습니다. dealType=" + request.getDealType() + ", targetId=" + request.getTargetId()
                ));
        return latestLog.getDeal();
    }

    private String waitingAdminStatus(ApprovalRequest request) {
        return "WAITING_ADMIN";
    }

    private ActionType toActionType(DecisionType decision) {
        return decision == DecisionType.REJECT ? ActionType.REJECT : ActionType.APPROVE;
    }

    private DealLogWriteService.DiffField diffField(String field, String label, Object before, Object after, String type) {
        return new DealLogWriteService.DiffField(field, label, before, after, type);
    }

    private record SubmitLogContext(
            SalesDeal deal,
            DealType docType,
            Long refId,
            String targetCode,
            DealStage fromStage,
            DealStage toStage,
            String fromStatus,
            String toStatus
    ) {
    }

}
