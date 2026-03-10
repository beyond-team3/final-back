package com.monsoon.seedflowplus.domain.approval.service;

import com.monsoon.seedflowplus.domain.schedule.dto.command.DealScheduleUpsertCommand;
import java.util.List;

public record ContractApprovalSchedulesSyncEvent(
        List<DealScheduleUpsertCommand> upsertCommands,
        List<String> deleteExternalKeys
) {
    public ContractApprovalSchedulesSyncEvent {
        upsertCommands = upsertCommands == null ? List.of() : List.copyOf(upsertCommands);
        deleteExternalKeys = deleteExternalKeys == null ? List.of() : List.copyOf(deleteExternalKeys);
    }
}
