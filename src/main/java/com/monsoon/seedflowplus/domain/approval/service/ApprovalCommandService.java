package com.monsoon.seedflowplus.domain.approval.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
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
import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.log.repository.SalesDealLogRepository;
import com.monsoon.seedflowplus.domain.deal.log.service.DocStatusTransitionValidator;
import com.monsoon.seedflowplus.domain.notification.event.ApprovalCompletedEvent;
import com.monsoon.seedflowplus.domain.notification.event.ApprovalRejectedEvent;
import com.monsoon.seedflowplus.domain.notification.event.ApprovalRequestedEvent;
import com.monsoon.seedflowplus.domain.notification.event.NotificationEventPublisher;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import com.monsoon.seedflowplus.domain.sales.order.dto.response.OrderResponse;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderHeader;
import com.monsoon.seedflowplus.domain.sales.order.repository.OrderHeaderRepository;
import com.monsoon.seedflowplus.domain.sales.order.service.OrderService;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationHeader;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.sales.quotation.repository.QuotationRepository;
import com.monsoon.seedflowplus.domain.sales.quotation.service.QuotationService;
import com.monsoon.seedflowplus.domain.sales.request.service.QuotationRequestService;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ApprovalCommandService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalStepRepository approvalStepRepository;
    private final ApprovalDecisionRepository approvalDecisionRepository;
    private final ApprovalDealLogWriter approvalDealLogWriter;
    private final ApprovalFlowPolicy approvalFlowPolicy;
    private final SalesDealLogRepository salesDealLogRepository;
    private final QuotationRepository quotationRepository;
    private final ContractRepository contractRepository;
    private final OrderHeaderRepository orderHeaderRepository;
    private final DocStatusTransitionValidator docStatusTransitionValidator;
    private final NotificationEventPublisher notificationEventPublisher;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final QuotationService quotationService;
    private final QuotationRequestService quotationRequestService;
    private final OrderService orderService;
    private final Clock clock;

    public CreateApprovalRequestResponse createApprovalRequest(
            CreateApprovalRequestRequest dto,
            CustomUserDetails principal
    ) {
        approvalFlowPolicy.validateSupportedDealType(dto.dealType());

        if (approvalRequestRepository.existsByDealTypeAndTargetIdAndStatus(
                dto.dealType(),
                dto.targetId(),
                ApprovalStatus.PENDING
        )) {
            throw new CoreException(ErrorType.APPROVAL_REQUEST_DUPLICATED);
        }

        ActorType submitActorType = determineActorTypeFromPrincipal(principal);
        validateSubmitOwnership(dto.dealType(), dto.targetId(), submitActorType, principal);
        Long clientIdSnapshot = resolveClientIdSnapshot(dto);
        validateCreateAccess(clientIdSnapshot, principal, submitActorType);

        ApprovalRequest request = ApprovalRequest.builder()
                .dealType(dto.dealType())
                .targetId(dto.targetId())
                .status(ApprovalStatus.PENDING)
                .clientIdSnapshot(clientIdSnapshot)
                .targetCodeSnapshot(dto.targetCodeSnapshot())
                .build();

        approvalFlowPolicy.createSteps(dto.dealType()).forEach(request::addStep);

        ApprovalRequest saved;
        try {
            saved = approvalRequestRepository.save(request);
        } catch (DataIntegrityViolationException e) {
            throw new CoreException(ErrorType.APPROVAL_REQUEST_DUPLICATED);
        }

        Long actorId = resolveActorIdByActorType(submitActorType, principal);
        approvalDealLogWriter.writeSubmit(saved, submitActorType, actorId);
        publishApprovalRequestedForFirstApprovers(saved, now());

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

        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new CoreException(ErrorType.APPROVAL_ALREADY_DECIDED);
        }

        validateStepActor(step, request, principal);
        validateStepOrder(step, request);
        validateRejectReason(dto);

        LocalDateTime now = now();
        String trimmedReason = StringUtils.hasText(dto.reason()) ? dto.reason().trim() : null;
        Long actorId = resolveActorIdByActorType(step.getActorType(), principal);
        DocumentDecisionResult documentDecisionResult = resolveAndApplyDocumentDecision(
                request,
                step,
                dto.decision(),
                now,
                actorId
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
        } else {
            step.approve(now);
        }

        updateRequestStatus(request, step, dto.decision());

        approvalDealLogWriter.writeDecision(
                request,
                step,
                dto.decision(),
                documentDecisionResult.fromStatus(),
                documentDecisionResult.toStatus(),
                documentDecisionResult.fromStage(),
                documentDecisionResult.toStage(),
                trimmedReason,
                step.getActorType(),
                actorId
        );
        publishContractApprovalSchedulesSyncAfterCommitIfNeeded(request, step, dto.decision(), now, principal);
        publishApprovalEventsAfterDecision(request, step, dto.decision(), now);

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
        Page<ApprovalRequest> page = searchAccessibleRequests(
                status,
                dealType,
                targetId,
                normalizeApprovalSearchPageable(pageable),
                principal
        );
        return page.map(this::toDetail);
    }

    private Pageable normalizeApprovalSearchPageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Order.desc("id")));
        }

        Sort normalizedSort = pageable.getSort().isSorted()
                ? Sort.by(pageable.getSort().stream()
                        .map(order -> "approvalId".equals(order.getProperty())
                                ? new Sort.Order(order.getDirection(), "id")
                                : order)
                        .toList())
                : Sort.by(Sort.Order.desc("id"));

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), normalizedSort);
    }

    private DocumentDecisionResult applyQuotationDecision(
            ApprovalRequest request,
            ApprovalStep step,
            DecisionType decision
    ) {
        QuotationHeader quotation = quotationRepository.findById(request.getTargetId())
                .orElseThrow(() -> new CoreException(ErrorType.QUOTATION_NOT_FOUND));

        String fromStatus = quotation.getStatus().name();
        String toStatus = switch (step.getActorType()) {
            case ADMIN -> decision == DecisionType.APPROVE
                    ? QuotationStatus.WAITING_CLIENT.name()
                    : QuotationStatus.REJECTED_ADMIN.name();
            case CLIENT -> decision == DecisionType.APPROVE
                    ? QuotationStatus.FINAL_APPROVED.name()
                    : QuotationStatus.REJECTED_CLIENT.name();
            default -> throw new CoreException(ErrorType.APPROVAL_ROLE_MISMATCH);
        };

        docStatusTransitionValidator.validateOrThrow(
                DealType.QUO,
                fromStatus,
                toActionType(decision),
                toStatus
        );

        quotation.updateStatus(QuotationStatus.valueOf(toStatus));

        return new DocumentDecisionResult(
                fromStatus,
                toStatus,
                toDealStageName(fromStatus),
                toDealStageName(toStatus)
        );
    }

    private DocumentDecisionResult applyContractDecision(
            ApprovalRequest request,
            ApprovalStep step,
            DecisionType decision,
            LocalDateTime actionAt,
            Long actorId
    ) {
        ContractHeader contract = contractRepository.findById(request.getTargetId())
                .orElseThrow(() -> new CoreException(ErrorType.CONTRACT_NOT_FOUND));

        String fromStatus = contract.getStatus().name();
        String toStatus = switch (step.getActorType()) {
            case ADMIN -> decision == DecisionType.APPROVE
                    ? ContractStatus.WAITING_CLIENT.name()
                    : ContractStatus.REJECTED_ADMIN.name();
            case CLIENT -> decision == DecisionType.APPROVE
                    ? ContractStatus.COMPLETED.name()
                    : ContractStatus.REJECTED_CLIENT.name();
            default -> throw new CoreException(ErrorType.APPROVAL_ROLE_MISMATCH);
        };

        docStatusTransitionValidator.validateOrThrow(
                DealType.CNT,
                fromStatus,
                toActionType(decision),
                toStatus
        );

        contract.updateStatus(ContractStatus.valueOf(toStatus));
        syncUpstreamDocumentsAfterContractDecision(contract, step, decision, actionAt, actorId);

        return new DocumentDecisionResult(
                fromStatus,
                toStatus,
                toDealStageName(fromStatus),
                toDealStageName(toStatus)
        );
    }

    private void syncUpstreamDocumentsAfterContractDecision(
            ContractHeader contract,
            ApprovalStep step,
            DecisionType decision,
            LocalDateTime actionAt,
            Long actorId
    ) {
        if (step.getActorType() != ActorType.CLIENT || decision != DecisionType.APPROVE) {
            return;
        }

        QuotationHeader quotation = contract.getQuotation();
        if (quotation == null) {
            return;
        }

        if (quotation.getStatus() == QuotationStatus.WAITING_CONTRACT) {
            quotationService.completeAfterContractApproval(quotation, ActorType.SYSTEM, null, actionAt);
        }

        if (quotation.getQuotationRequest() != null
                && quotation.getQuotationRequest().getStatus() == QuotationRequestStatus.REVIEWING) {
            quotationRequestService.completeAfterContractApproval(
                    quotation.getQuotationRequest(),
                    step.getActorType(),
                    actorId,
                    actionAt
            );
        }
    }

    private DocumentDecisionResult resolveAndApplyDocumentDecision(
            ApprovalRequest request,
            ApprovalStep step,
            DecisionType decision,
            LocalDateTime actionAt,
            Long actorId
    ) {
        return switch (request.getDealType()) {
            case QUO -> applyQuotationDecision(request, step, decision);
            case CNT -> applyContractDecision(request, step, decision, actionAt, actorId);
            case ORD -> applyOrderDecision(request, decision, actorId);
            default -> throw new CoreException(ErrorType.APPROVAL_UNSUPPORTED_DEAL_TYPE);
        };
    }

    private DocumentDecisionResult applyOrderDecision(
            ApprovalRequest request,
            DecisionType decision,
            Long actorId
    ) {
        OrderHeader orderHeader = orderHeaderRepository.findById(request.getTargetId())
                .orElseThrow(() -> new CoreException(ErrorType.ORDER_NOT_FOUND));

        String fromStatus = orderHeader.getStatus().name();
        String fromStage = DealStage.IN_PROGRESS.name();

        if (decision == DecisionType.REJECT) {
            return new DocumentDecisionResult(
                    fromStatus,
                    fromStatus,
                    fromStage,
                    fromStage
            );
        }

        CustomUserDetails approvalPrincipal = resolveOrderApprovalPrincipal(orderHeader, actorId);
        OrderResponse confirmedOrder = orderService.confirmOrder(orderHeader.getId(), approvalPrincipal);
        return new DocumentDecisionResult(
                fromStatus,
                confirmedOrder.getStatus().name(),
                fromStage,
                DealStage.CONFIRMED.name()
        );
    }

    private void publishContractApprovalSchedulesSyncAfterCommitIfNeeded(
            ApprovalRequest request,
            ApprovalStep step,
            DecisionType decision,
            LocalDateTime occurredAt,
            CustomUserDetails principal
    ) {
        if (request.getDealType() != DealType.CNT || step.getActorType() != ActorType.CLIENT || decision != DecisionType.APPROVE) {
            return;
        }

        ContractApprovalSchedulesSyncEvent event = new ContractApprovalSchedulesSyncEvent(
                request.getTargetId(),
                request.getDealType(),
                step.getStepOrder(),
                step.getActorType(),
                decision,
                occurredAt,
                principal == null ? null : principal.getUserId()
        );
        applicationEventPublisher.publishEvent(event);
    }

    private void validateStepOrder(ApprovalStep step, ApprovalRequest request) {
        if (step.getStepOrder() == 1) {
            return;
        }
        ApprovalStep previousStep = approvalStepRepository
                .findByApprovalRequestIdAndStepOrder(request.getId(), step.getStepOrder() - 1)
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

        if (step.getActorType() == ActorType.SALES_REP) {
            if (principal == null || principal.getRole() != Role.SALES_REP || principal.getEmployeeId() == null) {
                throw new CoreException(ErrorType.APPROVAL_ROLE_MISMATCH);
            }
            OrderHeader orderHeader = orderHeaderRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new CoreException(ErrorType.ORDER_NOT_FOUND));
            Long approverEmployeeId = resolveOrderApproverEmployeeId(orderHeader);
            if (!Objects.equals(approverEmployeeId, principal.getEmployeeId())) {
                throw new CoreException(ErrorType.APPROVAL_ROLE_MISMATCH);
            }
            return;
        }

        throw new CoreException(ErrorType.APPROVAL_ROLE_MISMATCH);
    }

    private void validateRejectReason(DecideApprovalRequest dto) {
        if (dto.decision() == DecisionType.REJECT && !StringUtils.hasText(dto.reason())) {
            throw new CoreException(ErrorType.APPROVAL_REASON_REQUIRED);
        }
    }

    private void updateRequestStatus(ApprovalRequest request, ApprovalStep step, DecisionType decision) {
        if (decision == DecisionType.REJECT) {
            request.reject();
            return;
        }
        if (approvalFlowPolicy.isLastStep(request.getDealType(), step.getStepOrder())) {
            request.approve();
        }
    }

    private ActorType determineActorTypeFromPrincipal(CustomUserDetails principal) {
        if (principal == null || principal.getRole() == null) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
        if (principal.getRole() == Role.SALES_REP) {
            if (principal.getEmployeeId() == null) {
                throw new CoreException(ErrorType.UNAUTHORIZED);
            }
            return ActorType.SALES_REP;
        }
        if (principal.getRole() == Role.ADMIN) {
            if (principal.getEmployeeId() == null) {
                throw new CoreException(ErrorType.UNAUTHORIZED);
            }
            return ActorType.ADMIN;
        }
        if (principal.getRole() == Role.CLIENT) {
            if (principal.getClientId() == null) {
                throw new CoreException(ErrorType.UNAUTHORIZED);
            }
            return ActorType.CLIENT;
        }
        throw new CoreException(ErrorType.UNAUTHORIZED);
    }

    private void validateCreateAccess(
            Long clientIdSnapshot,
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
        if (clientIdSnapshot == null) {
            throw new CoreException(ErrorType.APPROVAL_CLIENT_SNAPSHOT_REQUIRED);
        }
        if (!Objects.equals(clientIdSnapshot, principal.getClientId())) {
            throw new CoreException(ErrorType.APPROVAL_CLIENT_MISMATCH);
        }
    }

    private void validateSubmitOwnership(
            DealType dealType,
            Long targetId,
            ActorType submitActorType,
            CustomUserDetails principal
    ) {
        if (dealType == DealType.ORD) {
            Long orderClientId = orderHeaderRepository.findById(targetId)
                    .map(order -> order.getClient().getId())
                    .orElseThrow(() -> new CoreException(ErrorType.ORDER_NOT_FOUND));
            if (principal == null || principal.getClientId() == null || !Objects.equals(orderClientId, principal.getClientId())) {
                throw new CoreException(ErrorType.APPROVAL_CLIENT_MISMATCH);
            }
            return;
        }

        if (submitActorType != ActorType.SALES_REP) {
            return;
        }

        SalesDeal deal = switch (dealType) {
            case QUO -> quotationRepository.findById(targetId)
                    .map(QuotationHeader::getDeal)
                    .orElseThrow(() -> new CoreException(ErrorType.QUOTATION_NOT_FOUND));
            case CNT -> contractRepository.findById(targetId)
                    .map(ContractHeader::getDeal)
                    .orElseThrow(() -> new CoreException(ErrorType.CONTRACT_NOT_FOUND));
            default -> throw new CoreException(ErrorType.APPROVAL_UNSUPPORTED_DEAL_TYPE);
        };

        if (deal.getOwnerEmp() == null || deal.getOwnerEmp().getId() == null) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
        if (!Objects.equals(deal.getOwnerEmp().getId(), principal.getEmployeeId())) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
    }

    private Long resolveClientIdSnapshot(CreateApprovalRequestRequest dto) {
        if (dto.dealType() != DealType.QUO && dto.dealType() != DealType.CNT) {
            return dto.clientIdSnapshot();
        }

        Long actualClientId = switch (dto.dealType()) {
            case QUO -> quotationRepository.findById(dto.targetId())
                    .map(quotation -> quotation.getClient().getId())
                    .orElseThrow(() -> new CoreException(ErrorType.QUOTATION_NOT_FOUND));
            case CNT -> contractRepository.findById(dto.targetId())
                    .map(contract -> contract.getClient().getId())
                    .orElseThrow(() -> new CoreException(ErrorType.CONTRACT_NOT_FOUND));
            case ORD -> orderHeaderRepository.findById(dto.targetId())
                    .map(order -> order.getClient().getId())
                    .orElseThrow(() -> new CoreException(ErrorType.ORDER_NOT_FOUND));
            default -> throw new CoreException(ErrorType.APPROVAL_UNSUPPORTED_DEAL_TYPE);
        };

        if (dto.clientIdSnapshot() != null && !Objects.equals(dto.clientIdSnapshot(), actualClientId)) {
            throw new CoreException(ErrorType.APPROVAL_CLIENT_MISMATCH);
        }
        return actualClientId;
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

    private Page<ApprovalRequest> searchAccessibleRequests(
            ApprovalStatus status,
            DealType dealType,
            Long targetId,
            Pageable pageable,
            CustomUserDetails principal
    ) {
        if (principal == null || principal.getRole() == null) {
            return Page.empty(pageable);
        }
        if (principal.getRole() == Role.ADMIN) {
            return approvalRequestRepository.search(status, dealType, targetId, pageable);
        }
        if (principal.getRole() == Role.CLIENT) {
            if (principal.getClientId() == null) {
                throw new CoreException(ErrorType.UNAUTHORIZED);
            }
            return approvalRequestRepository.searchForClient(
                    status,
                    dealType,
                    targetId,
                    principal.getClientId(),
                    pageable
            );
        }
        if (principal.getRole() == Role.SALES_REP) {
            if (principal.getEmployeeId() == null) {
                throw new CoreException(ErrorType.UNAUTHORIZED);
            }
            return approvalRequestRepository.searchForSalesRep(
                    status,
                    dealType,
                    targetId,
                    principal.getEmployeeId(),
                    pageable
            );
        }
        return Page.empty(pageable);
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

    private ActionType toActionType(DecisionType decision) {
        return decision == DecisionType.REJECT ? ActionType.REJECT : ActionType.APPROVE;
    }

    private String toDealStageName(String status) {
        return switch (status) {
            case "WAITING_ADMIN" -> DealStage.PENDING_ADMIN.name();
            case "WAITING_CLIENT" -> DealStage.PENDING_CLIENT.name();
            case "REJECTED_ADMIN" -> DealStage.REJECTED_ADMIN.name();
            case "REJECTED_CLIENT" -> DealStage.REJECTED_CLIENT.name();
            case "FINAL_APPROVED", "COMPLETED" -> DealStage.APPROVED.name();
            default -> throw new CoreException(ErrorType.INVALID_DOC_STATUS_TRANSITION, "unsupported approval status=" + status);
        };
    }

    private record DocumentDecisionResult(
            String fromStatus,
            String toStatus,
            String fromStage,
            String toStage
    ) {
    }

    private void publishApprovalRequestedForFirstApprovers(ApprovalRequest request, LocalDateTime occurredAt) {
        request.getSteps().stream()
                .filter(step -> step.getStepOrder() == 1)
                .findFirst()
                .stream()
                .flatMap(step -> resolveApproverUserIds(step.getActorType(), request).stream())
                .forEach(userId ->
                notificationEventPublisher.publishAfterCommit(new ApprovalRequestedEvent(
                        userId,
                        request.getId(),
                        request.getDealType(),
                        request.getTargetId(),
                        occurredAt
                ))
        );
    }

    private void publishApprovalEventsAfterDecision(
            ApprovalRequest request,
            ApprovalStep step,
            DecisionType decision,
            LocalDateTime occurredAt
    ) {
        if (decision == DecisionType.REJECT) {
            resolveRequesterUserId(request).ifPresent(userId ->
                    notificationEventPublisher.publishAfterCommit(new ApprovalRejectedEvent(
                            userId,
                            request.getId(),
                            request.getDealType(),
                            request.getTargetId(),
                            occurredAt
                    ))
            );
            return;
        }

        if (request.getStatus() == ApprovalStatus.APPROVED) {
            resolveRequesterUserId(request).ifPresent(userId ->
                    notificationEventPublisher.publishAfterCommit(new ApprovalCompletedEvent(
                            userId,
                            request.getId(),
                            request.getDealType(),
                            request.getTargetId(),
                            occurredAt
                    ))
            );
            return;
        }

        approvalStepRepository
                .findByApprovalRequestIdAndStepOrder(request.getId(), step.getStepOrder() + 1)
                .filter(nextStep -> nextStep.getStatus() == ApprovalStepStatus.WAITING)
                .ifPresent(nextStep -> resolveApproverUserIds(nextStep.getActorType(), request)
                        .forEach(userId -> notificationEventPublisher.publishAfterCommit(new ApprovalRequestedEvent(
                                userId,
                                request.getId(),
                                request.getDealType(),
                                request.getTargetId(),
                                occurredAt
                        ))));
    }

    private List<Long> resolveApproverUserIds(ActorType actorType, ApprovalRequest request) {
        if (actorType == ActorType.ADMIN) {
            return userRepository.findAllByRole(Role.ADMIN).stream()
                    .map(user -> user.getId())
                    .toList();
        }
        if (actorType == ActorType.CLIENT && request.getClientIdSnapshot() != null) {
            return userRepository.findByClientId(request.getClientIdSnapshot())
                    .map(user -> List.of(user.getId()))
                    .orElseGet(List::of);
        }
        if (actorType == ActorType.SALES_REP) {
            return orderHeaderRepository.findById(request.getTargetId())
                    .map(this::resolveOrderApproverEmployeeId)
                    .flatMap(userRepository::findByEmployeeId)
                    .map(user -> List.of(user.getId()))
                    .orElseGet(List::of);
        }
        return List.of();
    }

    private java.util.Optional<Long> resolveRequesterUserId(ApprovalRequest request) {
        if (request.getDealType() == DealType.QUO) {
            return quotationRepository.findById(request.getTargetId())
                    .map(QuotationHeader::getDeal)
                    .map(SalesDeal::getOwnerEmp)
                    .map(owner -> owner == null ? null : owner.getId())
                    .flatMap(userRepository::findByEmployeeId)
                    .map(user -> user.getId());
        }
        if (request.getDealType() == DealType.CNT) {
            return contractRepository.findById(request.getTargetId())
                    .map(ContractHeader::getDeal)
                    .map(SalesDeal::getOwnerEmp)
                    .map(owner -> owner == null ? null : owner.getId())
                    .flatMap(userRepository::findByEmployeeId)
                    .map(user -> user.getId());
        }
        if (request.getDealType() == DealType.ORD) {
            return orderHeaderRepository.findById(request.getTargetId())
                    .map(OrderHeader::getClient)
                    .map(Client::getId)
                    .flatMap(userRepository::findByClientId)
                    .map(User::getId);
        }
        if (request.getClientIdSnapshot() != null) {
            return userRepository.findByClientId(request.getClientIdSnapshot()).map(user -> user.getId());
        }
        return java.util.Optional.empty();
    }

    private Long resolveOrderApproverEmployeeId(OrderHeader orderHeader) {
        if (orderHeader.getEmployee() != null && orderHeader.getEmployee().getId() != null) {
            return orderHeader.getEmployee().getId();
        }
        if (orderHeader.getDeal() != null
                && orderHeader.getDeal().getOwnerEmp() != null
                && orderHeader.getDeal().getOwnerEmp().getId() != null) {
            return orderHeader.getDeal().getOwnerEmp().getId();
        }
        throw new CoreException(ErrorType.UNAUTHORIZED);
    }

    private CustomUserDetails resolveOrderApprovalPrincipal(OrderHeader orderHeader, Long actorId) {
        Long approverEmployeeId = resolveOrderApproverEmployeeId(orderHeader);
        if (!Objects.equals(approverEmployeeId, actorId)) {
            throw new CoreException(ErrorType.APPROVAL_ROLE_MISMATCH);
        }
        return userRepository.findByEmployeeId(approverEmployeeId)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
