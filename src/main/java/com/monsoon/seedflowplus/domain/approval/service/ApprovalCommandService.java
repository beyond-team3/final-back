package com.monsoon.seedflowplus.domain.approval.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.approval.dto.request.CreateApprovalRequestRequest;
import com.monsoon.seedflowplus.domain.approval.dto.request.DecideApprovalRequest;
import com.monsoon.seedflowplus.domain.approval.dto.response.ApprovalDetailResponse;
import com.monsoon.seedflowplus.domain.approval.dto.response.ApprovalStepResponse;
import com.monsoon.seedflowplus.domain.approval.dto.response.CreateApprovalRequestResponse;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalDecision;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalRequest;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalStatus;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalStep;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalStepStatus;
import com.monsoon.seedflowplus.domain.approval.entity.DecisionType;
import com.monsoon.seedflowplus.domain.approval.repository.ApprovalDecisionRepository;
import com.monsoon.seedflowplus.domain.approval.repository.ApprovalRequestRepository;
import com.monsoon.seedflowplus.domain.approval.repository.ApprovalStepRepository;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.log.repository.SalesDealLogRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalCommandService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalStepRepository approvalStepRepository;
    private final ApprovalDecisionRepository approvalDecisionRepository;
    private final ApprovalDealLogWriter approvalDealLogWriter;
    private final SalesDealLogRepository salesDealLogRepository;

    public CreateApprovalRequestResponse createApprovalRequest(
            CreateApprovalRequestRequest dto,
            CustomUserDetails principal
    ) {
        validateSupportedDealType(dto.dealType());

        if (approvalRequestRepository.existsByDealTypeAndTargetIdAndStatus(
                dto.dealType(),
                dto.targetId(),
                ApprovalStatus.PENDING
        )) {
            throw new CoreException(ErrorType.APPROVAL_REQUEST_DUPLICATED);
        }

        if (dto.dealType() == DealType.CNT && dto.clientIdSnapshot() == null) {
            throw new CoreException(ErrorType.APPROVAL_CLIENT_SNAPSHOT_REQUIRED);
        }

        ActorType submitActorType = determineActorTypeFromPrincipal(principal);
        validateCreateAccess(dto, principal, submitActorType);

        ApprovalRequest request = ApprovalRequest.builder()
                .dealType(dto.dealType())
                .targetId(dto.targetId())
                .status(ApprovalStatus.PENDING)
                .clientIdSnapshot(dto.clientIdSnapshot())
                .targetCodeSnapshot(dto.targetCodeSnapshot())
                .build();

        request.addStep(ApprovalStep.builder()
                .stepOrder(1)
                .actorType(ActorType.ADMIN)
                .status(ApprovalStepStatus.WAITING)
                .build());

        if (dto.dealType() == DealType.CNT) {
            request.addStep(ApprovalStep.builder()
                    .stepOrder(2)
                    .actorType(ActorType.CLIENT)
                    .status(ApprovalStepStatus.WAITING)
                    .build());
        }

        ApprovalRequest saved;
        try {
            saved = approvalRequestRepository.save(request);
        } catch (DataIntegrityViolationException e) {
            throw new CoreException(ErrorType.APPROVAL_REQUEST_DUPLICATED);
        }

        Long actorId = resolveActorIdByActorType(submitActorType, principal);
        approvalDealLogWriter.writeSubmit(saved, submitActorType, actorId);

        return new CreateApprovalRequestResponse(
                saved.getId(),
                saved.getDealType(),
                saved.getTargetId(),
                saved.getStatus()
        );
    }

    public ApprovalDetailResponse decideStep(
            Long approvalId,
            Long stepId,
            DecideApprovalRequest dto,
            CustomUserDetails principal
    ) {
        ApprovalRequest request = approvalRequestRepository.findById(approvalId)
                .orElseThrow(() -> new CoreException(ErrorType.APPROVAL_NOT_FOUND));

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new CoreException(ErrorType.APPROVAL_ALREADY_DECIDED);
        }

        ApprovalStep step = approvalStepRepository.findByIdAndApprovalRequestIdForUpdate(stepId, approvalId)
                .orElseThrow(() -> new CoreException(ErrorType.APPROVAL_STEP_NOT_FOUND));

        if (step.getStatus() != ApprovalStepStatus.WAITING || approvalDecisionRepository.existsByApprovalStepId(step.getId())) {
            throw new CoreException(ErrorType.APPROVAL_ALREADY_DECIDED);
        }

        validateStepActor(step, request, principal);
        validateStepOrder(step, request);

        if (dto.decision() == DecisionType.REJECT && !StringUtils.hasText(dto.reason())) {
            throw new CoreException(ErrorType.APPROVAL_REASON_REQUIRED);
        }

        LocalDateTime now = LocalDateTime.now();
        String trimmedReason = StringUtils.hasText(dto.reason()) ? dto.reason().trim() : null;
        Long actorId = resolveActorIdByActorType(step.getActorType(), principal);

        approvalDealLogWriter.validateDecisionTransitionBeforeStatusChange(
                request,
                step,
                dto.decision(),
                step.getActorType()
        );

        try {
            approvalDecisionRepository.save(ApprovalDecision.builder()
                    .approvalStep(step)
                    .decision(dto.decision())
                    .reason(trimmedReason)
                    .decidedByUserId(actorId)
                    .decidedAt(now)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new CoreException(ErrorType.APPROVAL_ALREADY_DECIDED);
        }

        if (dto.decision() == DecisionType.REJECT) {
            step.reject(now);
            request.reject();
        } else {
            step.approve(now);
            if (request.getDealType() == DealType.QUO || step.getStepOrder() == 2) {
                request.approve();
            }
        }

        approvalDealLogWriter.writeDecision(
                request,
                step,
                dto.decision(),
                trimmedReason,
                step.getActorType(),
                actorId
        );

        return toDetail(request);
    }

    @Transactional(readOnly = true)
    public ApprovalDetailResponse getApproval(Long approvalId, CustomUserDetails principal) {
        ApprovalRequest request = approvalRequestRepository.findById(approvalId)
                .orElseThrow(() -> new CoreException(ErrorType.APPROVAL_NOT_FOUND));
        validateRequestAccess(request, principal);
        return toDetail(request);
    }

    @Transactional(readOnly = true)
    public Page<ApprovalDetailResponse> search(
            ApprovalStatus status,
            DealType dealType,
            Long targetId,
            Pageable pageable,
            CustomUserDetails principal
    ) {
        Page<ApprovalRequest> page = approvalRequestRepository.search(status, dealType, targetId, pageable);
        List<ApprovalDetailResponse> content = page.getContent().stream()
                .filter(request -> canAccess(request, principal))
                .map(this::toDetail)
                .toList();
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    private void validateSupportedDealType(DealType dealType) {
        if (dealType != DealType.QUO && dealType != DealType.CNT) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE, "approval supports only QUO/CNT");
        }
    }

    private void validateStepOrder(ApprovalStep step, ApprovalRequest request) {
        if (step.getStepOrder() == 1) {
            return;
        }
        if (step.getStepOrder() != 2) {
            throw new CoreException(ErrorType.APPROVAL_STEP_NOT_ACTIVE);
        }
        ApprovalStep previousStep = approvalStepRepository
                .findByApprovalRequestIdAndStepOrder(request.getId(), 1)
                .orElseThrow(() -> new CoreException(ErrorType.APPROVAL_STEP_NOT_FOUND));
        if (previousStep.getStatus() != ApprovalStepStatus.APPROVED) {
            throw new CoreException(ErrorType.APPROVAL_STEP_NOT_ACTIVE);
        }
    }

    private void validateStepActor(ApprovalStep step, ApprovalRequest request, CustomUserDetails principal) {
        if (step.getActorType() == ActorType.ADMIN) {
            if (principal == null || principal.getRole() != Role.ADMIN) {
                throw new CoreException(ErrorType.APPROVAL_ROLE_MISMATCH);
            }
            return;
        }

        if (step.getActorType() == ActorType.CLIENT) {
            if (principal == null || principal.getRole() != Role.CLIENT) {
                throw new CoreException(ErrorType.APPROVAL_ROLE_MISMATCH);
            }
            if (request.getClientIdSnapshot() == null
                    || principal.getClientId() == null
                    || !request.getClientIdSnapshot().equals(principal.getClientId())) {
                throw new CoreException(ErrorType.APPROVAL_CLIENT_MISMATCH);
            }
            return;
        }

        throw new CoreException(ErrorType.APPROVAL_ROLE_MISMATCH);
    }

    private ActorType determineActorTypeFromPrincipal(CustomUserDetails principal) {
        if (principal == null || principal.getRole() == null) {
            return ActorType.SYSTEM;
        }
        if (principal.getRole() == Role.SALES_REP) {
            if (principal.getUserId() == null) {
                throw new CoreException(ErrorType.UNAUTHORIZED);
            }
            return ActorType.SALES_REP;
        }
        return ActorType.SYSTEM;
    }

    private void validateCreateAccess(
            CreateApprovalRequestRequest dto,
            CustomUserDetails principal,
            ActorType submitActorType
    ) {
        if (principal == null || principal.getRole() == null) {
            return;
        }
        boolean needsClientSnapshotValidation = submitActorType != ActorType.SALES_REP || principal.getRole() == Role.CLIENT;
        if (!needsClientSnapshotValidation) {
            return;
        }
        if (principal.getClientId() == null) {
            return;
        }
        if (dto.clientIdSnapshot() == null) {
            throw new CoreException(ErrorType.APPROVAL_CLIENT_SNAPSHOT_REQUIRED);
        }
        if (!Objects.equals(dto.clientIdSnapshot(), principal.getClientId())) {
            throw new CoreException(ErrorType.APPROVAL_CLIENT_MISMATCH);
        }
    }

    private Long resolveActorIdByActorType(ActorType actorType, CustomUserDetails principal) {
        if (actorType == ActorType.SYSTEM) {
            return null;
        }
        if (principal == null) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
        if (actorType == ActorType.CLIENT) {
            if (principal.getClientId() == null) {
                throw new CoreException(ErrorType.UNAUTHORIZED);
            }
            return principal.getClientId();
        }
        if (actorType == ActorType.SALES_REP || actorType == ActorType.ADMIN) {
            if (principal.getEmployeeId() == null) {
                throw new CoreException(ErrorType.UNAUTHORIZED);
            }
            return principal.getEmployeeId();
        }
        throw new CoreException(ErrorType.UNAUTHORIZED);
    }

    private ApprovalDetailResponse toDetail(ApprovalRequest request) {
        List<ApprovalStepResponse> steps = approvalStepRepository
                .findByApprovalRequestIdOrderByStepOrderAsc(request.getId())
                .stream()
                .map(step -> {
                    ApprovalDecision decision = approvalDecisionRepository.findByApprovalStepId(step.getId()).orElse(null);
                    return new ApprovalStepResponse(
                            step.getId(),
                            step.getStepOrder(),
                            step.getActorType(),
                            step.getStatus(),
                            step.getDecidedAt(),
                            decision == null ? null : decision.getDecision(),
                            decision == null ? null : decision.getReason(),
                            decision == null ? null : decision.getDecidedByUserId()
                    );
                })
                .toList();

        return new ApprovalDetailResponse(
                request.getId(),
                request.getDealType(),
                request.getTargetId(),
                request.getStatus(),
                request.getClientIdSnapshot(),
                request.getTargetCodeSnapshot(),
                steps
        );
    }

    private void validateRequestAccess(ApprovalRequest request, CustomUserDetails principal) {
        if (!canAccess(request, principal)) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
    }

    private boolean canAccess(ApprovalRequest request, CustomUserDetails principal) {
        if (principal == null || principal.getRole() == null) {
            return false;
        }
        if (principal.getRole() == Role.ADMIN) {
            return true;
        }
        if (principal.getRole() == Role.CLIENT) {
            if (request.getClientIdSnapshot() != null && principal.getClientId() != null) {
                return Objects.equals(request.getClientIdSnapshot(), principal.getClientId());
            }
            return salesDealLogRepository
                    .findTopByDocTypeAndRefIdOrderByActionAtDescIdDesc(request.getDealType(), request.getTargetId())
                    .map(log -> log != null
                            && log.getClient() != null
                            && principal.getClientId() != null
                            && Objects.equals(log.getClient().getId(), principal.getClientId()))
                    .orElse(false);
        }
        if (principal.getRole() == Role.SALES_REP) {
            return salesDealLogRepository
                    .findTopByDocTypeAndRefIdOrderByActionAtDescIdDesc(request.getDealType(), request.getTargetId())
                    .map(log -> log != null
                            && log.getDeal() != null
                            && log.getDeal().getOwnerEmp() != null
                            && principal.getEmployeeId() != null
                            && Objects.equals(log.getDeal().getOwnerEmp().getId(), principal.getEmployeeId()))
                    .orElse(false);
        }
        return false;
    }

    private record DocumentDecisionResult(
            String fromStatus,
            String toStatus,
            String fromStage,
            String toStage
    ) {
    }
}
