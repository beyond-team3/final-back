package com.monsoon.seedflowplus.domain.approval.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.approval.dto.request.CreateApprovalRequestRequest;
import com.monsoon.seedflowplus.domain.approval.dto.response.CreateApprovalRequestResponse;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalRequest;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalStatus;
import com.monsoon.seedflowplus.domain.approval.repository.ApprovalRequestRepository;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.notification.event.ApprovalRequestedEvent;
import com.monsoon.seedflowplus.domain.notification.event.NotificationEventPublisher;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderHeader;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderStatus;
import com.monsoon.seedflowplus.domain.sales.order.repository.OrderHeaderRepository;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationHeader;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.sales.quotation.repository.QuotationRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalSubmissionService {

    private final ApprovalRequestRepository approvalRequestRepository;
    private final ApprovalDealLogWriter approvalDealLogWriter;
    private final ApprovalFlowPolicy approvalFlowPolicy;
    private final QuotationRepository quotationRepository;
    private final ContractRepository contractRepository;
    private final OrderHeaderRepository orderHeaderRepository;
    private final NotificationEventPublisher notificationEventPublisher;
    private final UserRepository userRepository;
    private final Clock clock;

    public CreateApprovalRequestResponse createApprovalRequest(
            CreateApprovalRequestRequest dto,
            CustomUserDetails principal
    ) {
        validateManualCreateAccess(principal);
        return submit(dto, principal);
    }

    public CreateApprovalRequestResponse submitFromDocumentCreation(
            DealType dealType,
            Long targetId,
            String targetCodeSnapshot,
            CustomUserDetails principal
    ) {
        return submit(new CreateApprovalRequestRequest(dealType, targetId, null, targetCodeSnapshot), principal);
    }

    private CreateApprovalRequestResponse submit(
            CreateApprovalRequestRequest dto,
            CustomUserDetails principal
    ) {
        approvalFlowPolicy.validateSupportedDealType(dto.dealType());
        validateDocumentReadyForSubmission(dto.dealType(), dto.targetId());

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
        String targetCodeSnapshot = resolveTargetCodeSnapshot(dto);
        validateCreateAccess(clientIdSnapshot, principal, submitActorType);

        ApprovalRequest request = ApprovalRequest.builder()
                .dealType(dto.dealType())
                .targetId(dto.targetId())
                .status(ApprovalStatus.PENDING)
                .clientIdSnapshot(clientIdSnapshot)
                .targetCodeSnapshot(targetCodeSnapshot)
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

    private void validateManualCreateAccess(CustomUserDetails principal) {
        if (principal == null || principal.getRole() != Role.ADMIN) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }
        if (principal.getEmployeeId() == null) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
    }

    private void validateDocumentReadyForSubmission(DealType dealType, Long targetId) {
        switch (dealType) {
            case QUO -> {
                QuotationStatus status = quotationRepository.findById(targetId)
                        .map(QuotationHeader::getStatus)
                        .orElseThrow(() -> new CoreException(ErrorType.QUOTATION_NOT_FOUND));
                if (status != QuotationStatus.WAITING_ADMIN) {
                    throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS);
                }
            }
            case CNT -> {
                ContractStatus status = contractRepository.findById(targetId)
                        .map(ContractHeader::getStatus)
                        .orElseThrow(() -> new CoreException(ErrorType.CONTRACT_NOT_FOUND));
                if (status != ContractStatus.WAITING_ADMIN) {
                    throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS);
                }
            }
            case ORD -> {
                OrderStatus status = orderHeaderRepository.findById(targetId)
                        .map(OrderHeader::getStatus)
                        .orElseThrow(() -> new CoreException(ErrorType.ORDER_NOT_FOUND));
                if (status != OrderStatus.PENDING) {
                    throw new CoreException(ErrorType.INVALID_DOCUMENT_STATUS);
                }
            }
            default -> throw new CoreException(ErrorType.APPROVAL_UNSUPPORTED_DEAL_TYPE);
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

    private String resolveTargetCodeSnapshot(CreateApprovalRequestRequest dto) {
        if (dto.targetCodeSnapshot() != null && !dto.targetCodeSnapshot().isBlank()) {
            return dto.targetCodeSnapshot();
        }
        return switch (dto.dealType()) {
            case QUO -> quotationRepository.findById(dto.targetId())
                    .map(QuotationHeader::getQuotationCode)
                    .orElseThrow(() -> new CoreException(ErrorType.QUOTATION_NOT_FOUND));
            case CNT -> contractRepository.findById(dto.targetId())
                    .map(ContractHeader::getContractCode)
                    .orElseThrow(() -> new CoreException(ErrorType.CONTRACT_NOT_FOUND));
            case ORD -> orderHeaderRepository.findById(dto.targetId())
                    .map(OrderHeader::getOrderCode)
                    .orElseThrow(() -> new CoreException(ErrorType.ORDER_NOT_FOUND));
            default -> throw new CoreException(ErrorType.APPROVAL_UNSUPPORTED_DEAL_TYPE);
        };
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

    private void publishApprovalRequestedForFirstApprovers(ApprovalRequest request, LocalDateTime occurredAt) {
        request.getSteps().stream()
                .filter(step -> step.getStepOrder() == 1)
                .findFirst()
                .ifPresent(step -> resolveApproverUserIds(step.getActorType(), request)
                        .forEach(userId -> notificationEventPublisher.publishAfterCommit(new ApprovalRequestedEvent(
                                userId,
                                request.getId(),
                                request.getDealType(),
                                request.getTargetId(),
                                request.getTargetCodeSnapshot(),
                                step.getActorType(),
                                occurredAt
                        ))));
    }

    private List<Long> resolveApproverUserIds(ActorType actorType, ApprovalRequest request) {
        if (actorType == ActorType.ADMIN) {
            return userRepository.findAllByRole(Role.ADMIN).stream()
                    .map(User::getId)
                    .toList();
        }
        if (actorType == ActorType.SALES_REP) {
            return orderHeaderRepository.findById(request.getTargetId())
                    .map(OrderHeader::getDeal)
                    .map(SalesDeal::getOwnerEmp)
                    .map(owner -> owner == null ? null : owner.getId())
                    .flatMap(userRepository::findByEmployeeId)
                    .map(user -> List.of(user.getId()))
                    .orElseGet(List::of);
        }
        if (actorType == ActorType.CLIENT && request.getClientIdSnapshot() != null) {
            return userRepository.findByClientId(request.getClientIdSnapshot())
                    .map(user -> List.of(user.getId()))
                    .orElseGet(List::of);
        }
        return List.of();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
