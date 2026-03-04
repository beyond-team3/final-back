package com.monsoon.seedflowplus.domain.approval.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApprovalDealLogWriter {

    private final SalesDealLogRepository salesDealLogRepository;
    private final DealLogWriteService dealLogWriteService;
    private final ObjectMapper objectMapper;

    public void writeSubmit(ApprovalRequest request, ActorType actorType, Long actorId) {
        SalesDeal deal = resolveDeal(request);
        dealLogWriteService.write(
                deal,
                request.getDealType(),
                request.getTargetId(),
                request.getTargetCodeSnapshot(),
                deal.getCurrentStage(),
                DealStage.PENDING_ADMIN,
                waitingAdminStatus(request),
                waitingAdminStatus(request),
                ActionType.SUBMIT,
                null,
                actorType,
                actorId,
                null,
                buildDiffJson(request.getId(), null, null)
        );
    }

    public void writeDecision(
            ApprovalRequest request,
            ApprovalStep step,
            DecisionType decision,
            String reason,
            ActorType actorType,
            Long actorId
    ) {
        SalesDeal deal = resolveDeal(request);
        boolean rejected = decision == DecisionType.REJECT;

        DealStage toStage;
        String toStatus;
        String fromStatus = fromStatus(request, step);
        if (rejected) {
            if (actorType == ActorType.ADMIN) {
                toStage = DealStage.REJECTED_ADMIN;
                toStatus = rejectedAdminStatus(request);
            } else {
                toStage = DealStage.REJECTED_CLIENT;
                toStatus = rejectedClientStatus(request);
            }
        } else if (request.getDealType() == DealType.QUO || step.getStepOrder() == 2) {
            toStage = DealStage.APPROVED;
            toStatus = approvedStatus(request);
        } else {
            toStage = DealStage.PENDING_CLIENT;
            toStatus = waitingClientStatus(request);
        }

        dealLogWriteService.write(
                deal,
                request.getDealType(),
                request.getTargetId(),
                request.getTargetCodeSnapshot(),
                deal.getCurrentStage(),
                toStage,
                fromStatus,
                toStatus,
                rejected ? ActionType.REJECT : ActionType.APPROVE,
                null,
                actorType,
                actorId,
                reason,
                buildDiffJson(request.getId(), step.getStepOrder(), decision)
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

    private String fromStatus(ApprovalRequest request, ApprovalStep step) {
        if (step.getStepOrder() == 1) {
            return waitingAdminStatus(request);
        }
        return waitingClientStatus(request);
    }

    private String waitingAdminStatus(ApprovalRequest request) {
        return "WAITING_ADMIN";
    }

    private String waitingClientStatus(ApprovalRequest request) {
        return "WAITING_CLIENT";
    }

    private String rejectedAdminStatus(ApprovalRequest request) {
        return "REJECTED_ADMIN";
    }

    private String rejectedClientStatus(ApprovalRequest request) {
        return "REJECTED_CLIENT";
    }

    private String approvedStatus(ApprovalRequest request) {
        return request.getDealType() == DealType.QUO ? "FINAL_APPROVED" : "COMPLETED";
    }

    private String buildDiffJson(Long approvalRequestId, Integer stepOrder, DecisionType decision) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("approvalRequestId", approvalRequestId);
            payload.put("stepOrder", stepOrder);
            payload.put("decision", decision == null ? "SUBMIT" : decision.name());
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{\"approvalRequestId\":" + approvalRequestId + "}";
        }
    }
}
