package com.monsoon.seedflowplus.domain.approval.service;

import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.approval.entity.DecisionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import com.monsoon.seedflowplus.domain.schedule.dto.command.DealScheduleUpsertCommand;
import com.monsoon.seedflowplus.domain.schedule.entity.DealDocType;
import com.monsoon.seedflowplus.domain.schedule.entity.DealScheduleEventType;
import com.monsoon.seedflowplus.domain.schedule.sync.DealScheduleSyncService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class ContractApprovalSchedulesSyncEventHandler {

    private final ContractRepository contractRepository;
    private final UserRepository userRepository;
    private final DealScheduleSyncService dealScheduleSyncService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ContractApprovalSchedulesSyncEvent event) {
        if (!supports(event)) {
            return;
        }

        try {
            contractRepository.findByIdWithScheduleRelations(event.targetId()).ifPresentOrElse(
                    contract -> syncContractSchedules(contract, event),
                    () -> log.warn("Skipping contract approval schedule sync because contract was not found. contractId={}", event.targetId())
            );
        } catch (Throwable t) {
            log.error("Failed to handle contract approval schedule sync event. event={}", event, t);
        }
    }

    private boolean supports(ContractApprovalSchedulesSyncEvent event) {
        return event.dealType() == DealType.CNT
                && event.actorType() == ActorType.CLIENT
                && event.decision() == DecisionType.APPROVE;
    }

    private void syncContractSchedules(ContractHeader contract, ContractApprovalSchedulesSyncEvent event) {
        if (contract.getDeal() == null || contract.getDeal().getId() == null) {
            log.warn("Skipping contract approval schedule sync because deal is missing. contractId={}", contract.getId());
            return;
        }

        String startExternalKey = externalKey(contract.getId(), "START");
        String endExternalKey = externalKey(contract.getId(), "END");
        if (contract.getStartDate() == null) {
            dealScheduleSyncService.deleteByExternalKey(startExternalKey);
        }
        if (contract.getEndDate() == null) {
            dealScheduleSyncService.deleteByExternalKey(endExternalKey);
        }

        Long assigneeUserId = resolveScheduleAssigneeUserId(contract.getDeal(), event.principalUserId(), contract.getClient().getId())
                .orElse(null);
        if (assigneeUserId == null) {
            if (contract.getStartDate() != null || contract.getEndDate() != null) {
                log.warn("Skipping contract approval schedule upsert because assignee user could not be resolved. contractId={}", contract.getId());
            }
            return;
        }

        if (contract.getStartDate() != null) {
            dealScheduleSyncService.upsertFromEvent(new DealScheduleUpsertCommand(
                    startExternalKey,
                    contract.getDeal().getId(),
                    contract.getClient().getId(),
                    assigneeUserId,
                    DealScheduleEventType.DOC_APPROVED,
                    DealDocType.CNT,
                    contract.getId(),
                    null,
                    "계약 시작일: " + contract.getClient().getClientName(),
                    null,
                    contract.getStartDate().atStartOfDay(),
                    contract.getStartDate().plusDays(1).atStartOfDay(),
                    event.occurredAt()
            ));
        }

        if (contract.getEndDate() != null) {
            dealScheduleSyncService.upsertFromEvent(new DealScheduleUpsertCommand(
                    endExternalKey,
                    contract.getDeal().getId(),
                    contract.getClient().getId(),
                    assigneeUserId,
                    DealScheduleEventType.DOC_APPROVED,
                    DealDocType.CNT,
                    contract.getId(),
                    null,
                    "계약 만료일: " + contract.getClient().getClientName(),
                    null,
                    contract.getEndDate().atStartOfDay(),
                    contract.getEndDate().plusDays(1).atStartOfDay(),
                    event.occurredAt()
            ));
        }
    }

    private Optional<Long> resolveScheduleAssigneeUserId(SalesDeal deal, Long principalUserId, Long clientId) {
        if (deal.getOwnerEmp() != null && deal.getOwnerEmp().getId() != null) {
            Optional<Long> ownerUserId = userRepository.findByEmployeeId(deal.getOwnerEmp().getId())
                    .map(User::getId);
            if (ownerUserId.isPresent()) {
                return ownerUserId;
            }
        }
        if (principalUserId != null) {
            return Optional.of(principalUserId);
        }
        if (clientId != null) {
            return userRepository.findByClientId(clientId).map(User::getId);
        }
        return Optional.empty();
    }

    private String externalKey(Long contractId, String scheduleBoundary) {
        return "CNT_" + contractId + "_DOC_APPROVED_" + scheduleBoundary;
    }
}
