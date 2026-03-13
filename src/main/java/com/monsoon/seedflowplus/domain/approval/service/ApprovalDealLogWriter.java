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
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApprovalDealLogWriter {

    private final SalesDealLogRepository salesDealLogRepository;
    private final DealLogWriteService dealLogWriteService;
    private final DealPipelineFacade dealPipelineFacade;

    public void writeSubmit(ApprovalRequest request, ActorType actorType, Long actorId) {
        SubmitLogContext context = buildSubmitContext(request);
        dealLogWriteService.write(
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
        SalesDeal deal = resolveDeal(request);
        if (request.getDealType() == DealType.ORD) {
            writeOrderDecisionLog(
                    request,
                    step,
                    decision,
                    fromStatus,
                    toStatus,
                    fromStage,
                    toStage,
                    reason,
                    actorType,
                    actorId,
                    deal
            );
            return;
        }
        DealStage actualFromStage = resolveDecisionFromStage(deal, fromStage);

        dealPipelineFacade.recordAndSync(
                deal,
                request.getDealType(),
                request.getTargetId(),
                request.getTargetCodeSnapshot(),
                actualFromStage,
                parseDealStageOrThrow(toStage),
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

    private void writeOrderDecisionLog(
            ApprovalRequest request,
            ApprovalStep step,
            DecisionType decision,
            String fromStatus,
            String toStatus,
            String fromStage,
            String toStage,
            String reason,
            ActorType actorType,
            Long actorId,
            SalesDeal deal
    ) {
        dealLogWriteService.write(
                deal,
                request.getDealType(),
                request.getTargetId(),
                request.getTargetCodeSnapshot(),
                parseDealStageOrThrow(fromStage),
                parseDealStageOrThrow(toStage),
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

    private DealStage resolveDecisionFromStage(SalesDeal deal, String fromStage) {
        DealStage actualFromStage = deal.getCurrentStage();
        if (actualFromStage == null) {
            throw new CoreException(ErrorType.DEAL_NOT_FOUND, "deal currentStage가 존재하지 않습니다. dealId=" + deal.getId());
        }
        if (fromStage == null) {
            return actualFromStage;
        }
        DealStage expectedFromStage = parseDealStageOrThrow(fromStage);
        if (expectedFromStage != actualFromStage) {
            throw new CoreException(
                    ErrorType.INVALID_DOC_STATUS_TRANSITION,
                    "approval decision fromStage mismatch. expected="
                            + expectedFromStage
                            + ", actual="
                            + actualFromStage
                            + ", dealId="
                            + deal.getId()
            );
        }
        return actualFromStage;
    }

    private DealStage parseDealStageOrThrow(String stageName) {
        try {
            return DealStage.valueOf(stageName);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CoreException(
                    ErrorType.INVALID_DOC_STATUS_TRANSITION,
                    "invalid approval decision stage. stage=" + stageName
            );
        }
    }

    private SubmitLogContext buildSubmitContext(ApprovalRequest request) {
        SalesDeal deal = resolveDeal(request);
        if (request.getDealType() == DealType.ORD) {
            return new SubmitLogContext(
                    deal,
                    request.getDealType(),
                    request.getTargetId(),
                    request.getTargetCodeSnapshot(),
                    deal.getCurrentStage(),
                    deal.getCurrentStage(),
                    OrderStatus.PENDING.name(),
                    OrderStatus.PENDING.name()
            );
        }
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
