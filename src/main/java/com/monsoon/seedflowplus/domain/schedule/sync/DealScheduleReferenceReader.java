package com.monsoon.seedflowplus.domain.schedule.sync;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;

public interface DealScheduleReferenceReader {

    DealScheduleReferences loadForSync(Long dealId, Long clientId, Long assigneeUserId);

    record DealScheduleReferences(
            SalesDeal deal,
            Client client,
            User assignee
    ) {
    }
}
