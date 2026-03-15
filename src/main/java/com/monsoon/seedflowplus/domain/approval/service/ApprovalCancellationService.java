package com.monsoon.seedflowplus.domain.approval.service;

import com.monsoon.seedflowplus.domain.approval.entity.ApprovalStatus;
import com.monsoon.seedflowplus.domain.approval.repository.ApprovalRequestRepository;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ApprovalCancellationService {

    private final ApprovalRequestRepository approvalRequestRepository;

    public void cancelPendingRequest(DealType dealType, Long targetId) {
        approvalRequestRepository.findByDealTypeAndTargetIdAndStatus(dealType, targetId, ApprovalStatus.PENDING)
                .ifPresent(request -> request.cancel());
    }
}
