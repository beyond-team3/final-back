package com.monsoon.seedflowplus.domain.schedule.sync;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.schedule.dto.command.DealScheduleUpsertCommand;
import com.monsoon.seedflowplus.domain.schedule.entity.DealSchedule;
import com.monsoon.seedflowplus.domain.schedule.entity.ScheduleSource;
import com.monsoon.seedflowplus.domain.schedule.repository.DealScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DealScheduleSyncService {

    private final DealScheduleRepository dealScheduleRepository;
    private final DealScheduleReferenceReader dealScheduleReferenceReader;

    @Transactional
    public Long upsertFromEvent(DealScheduleUpsertCommand command) {
        validateCommand(command);

        DealScheduleReferenceReader.DealScheduleReferences references =
                dealScheduleReferenceReader.loadForSync(command.dealId(), command.clientId(), command.assigneeUserId());
        SalesDeal deal = references.deal();
        Client client = references.client();
        User assignee = references.assignee();
        validateDealClientMatch(deal, client);

        try {
            DealSchedule schedule = dealScheduleRepository.findByExternalKey(command.externalKey())
                    .map(existing -> {
                        existing.syncUpdate(
                                assignee,
                                command.title(),
                                command.description(),
                                command.startAt(),
                                command.endAt(),
                                command.eventType(),
                                command.docType(),
                                command.refDocId(),
                                command.refDealLogId(),
                                ScheduleSource.AUTO_SYNC,
                                command.lastSyncedAt()
                        );
                        return existing;
                    })
                    .orElseGet(() -> DealSchedule.builder()
                            .deal(deal)
                            .client(client)
                            .assigneeUser(assignee)
                            .title(command.title())
                            .description(command.description())
                            .startAt(command.startAt())
                            .endAt(command.endAt())
                            .eventType(command.eventType())
                            .docType(command.docType())
                            .refDocId(command.refDocId())
                            .refDealLogId(command.refDealLogId())
                            .source(ScheduleSource.AUTO_SYNC)
                            .externalKey(command.externalKey())
                            .lastSyncedAt(command.lastSyncedAt())
                            .build());

            return dealScheduleRepository.saveAndFlush(schedule).getId();
        } catch (DataIntegrityViolationException e) {
            DealSchedule existing = dealScheduleRepository.findByExternalKey(command.externalKey())
                    .orElseThrow(() -> e);
            existing.syncUpdate(
                    assignee,
                    command.title(),
                    command.description(),
                    command.startAt(),
                    command.endAt(),
                    command.eventType(),
                    command.docType(),
                    command.refDocId(),
                    command.refDealLogId(),
                    ScheduleSource.AUTO_SYNC,
                    command.lastSyncedAt()
            );
            return dealScheduleRepository.save(existing).getId();
        }
    }

    private void validateCommand(DealScheduleUpsertCommand command) {
        if (command == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (command.externalKey() == null || command.externalKey().isBlank()) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (command.dealId() == null || command.clientId() == null || command.assigneeUserId() == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (command.eventType() == null || command.docType() == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (command.title() == null || command.title().isBlank()) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (command.startAt() == null || command.endAt() == null || !command.startAt().isBefore(command.endAt())) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (command.lastSyncedAt() == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
    }

    private void validateDealClientMatch(SalesDeal deal, Client client) {
        if (deal.getClient() == null || deal.getClient().getId() == null || !deal.getClient().getId().equals(client.getId())) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE, "deal and client mismatch");
        }
    }
}
