package com.monsoon.seedflowplus.domain.approval.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.approval.entity.ApprovalRequest;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalStatus;
import com.monsoon.seedflowplus.domain.approval.repository.ApprovalRequestRepository;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApprovalCancellationServiceTest {

    @Mock
    private ApprovalRequestRepository approvalRequestRepository;

    @InjectMocks
    private ApprovalCancellationService approvalCancellationService;

    @Test
    @DisplayName("삭제 시 진행 중 승인 요청은 CANCELED로 전환된다")
    void cancelPendingRequestCancelsApproval() {
        ApprovalRequest request = ApprovalRequest.builder()
                .dealType(DealType.QUO)
                .targetId(10L)
                .status(ApprovalStatus.PENDING)
                .clientIdSnapshot(1L)
                .targetCodeSnapshot("QUO-10")
                .build();
        when(approvalRequestRepository.findByDealTypeAndTargetIdAndStatus(DealType.QUO, 10L, ApprovalStatus.PENDING))
                .thenReturn(Optional.of(request));

        approvalCancellationService.cancelPendingRequest(DealType.QUO, 10L);

        assertThat(request.getStatus()).isEqualTo(ApprovalStatus.CANCELED);
    }

    @Test
    @DisplayName("삭제 대상에 진행 중 승인 요청이 없으면 아무 작업도 하지 않는다")
    void cancelPendingRequestNoOpsWhenNothingPending() {
        when(approvalRequestRepository.findByDealTypeAndTargetIdAndStatus(DealType.CNT, 20L, ApprovalStatus.PENDING))
                .thenReturn(Optional.empty());

        approvalCancellationService.cancelPendingRequest(DealType.CNT, 20L);

        verify(approvalRequestRepository).findByDealTypeAndTargetIdAndStatus(DealType.CNT, 20L, ApprovalStatus.PENDING);
        verify(approvalRequestRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
