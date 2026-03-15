package com.monsoon.seedflowplus.domain.schedule.sync;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DealScheduleReferenceReaderImpl implements DealScheduleReferenceReader {

    private final SalesDealRepository salesDealRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    @Override
    public DealScheduleReferences loadForSync(Long dealId, Long clientId, Long assigneeUserId) {
        SalesDeal deal = salesDealRepository.findById(dealId)
                .orElseThrow(() -> new CoreException(ErrorType.DEAL_NOT_FOUND));
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));
        User assignee = userRepository.findById(assigneeUserId)
                .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));
        return new DealScheduleReferences(deal, client, assignee);
    }
}
