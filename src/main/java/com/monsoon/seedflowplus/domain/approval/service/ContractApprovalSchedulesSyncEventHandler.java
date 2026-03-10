package com.monsoon.seedflowplus.domain.approval.service;

import com.monsoon.seedflowplus.domain.schedule.sync.DealScheduleSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContractApprovalSchedulesSyncEventHandler {

    private final DealScheduleSyncService dealScheduleSyncService;

    @EventListener
    public void handle(ContractApprovalSchedulesSyncEvent event) {
        event.deleteExternalKeys().forEach(dealScheduleSyncService::deleteByExternalKey);
        event.upsertCommands().forEach(dealScheduleSyncService::upsertFromEvent);
    }
}
