package com.monsoon.seedflowplus.domain.approval.service;

import com.monsoon.seedflowplus.domain.approval.entity.ApprovalRequest;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalStep;
import com.monsoon.seedflowplus.domain.approval.entity.ApprovalStepStatus;
import com.monsoon.seedflowplus.domain.approval.entity.DecisionType;
import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.log.entity.SalesDealLog;
import com.monsoon.seedflowplus.domain.deal.log.repository.SalesDealLogRepository;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogWriteService;
import com.monsoon.seedflowplus.domain.deal.log.service.DealPipelineFacade;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalDealLogWriterTest {

    @Mock
    private SalesDealLogRepository salesDealLogRepository;

    @Mock
    private DealPipelineFacade dealPipelineFacade;

    @InjectMocks
    private ApprovalDealLogWriter approvalDealLogWriter;

    @Test
    @DisplayName("writeDecision은 전달받은 fromStage와 달라도 실제 deal currentStage를 기록한다")
    void writeDecisionUsesActualDealStageAsFromStage() {
        ApprovalRequest request = ApprovalRequest.builder()
                .dealType(DealType.QUO)
                .targetId(100L)
                .status(com.monsoon.seedflowplus.domain.approval.entity.ApprovalStatus.PENDING)
                .clientIdSnapshot(10L)
                .targetCodeSnapshot("Q-100")
                .build();
        ReflectionTestUtils.setField(request, "id", 1L);

        ApprovalStep step = ApprovalStep.builder()
                .approvalRequest(request)
                .stepOrder(1)
                .actorType(ActorType.ADMIN)
                .status(ApprovalStepStatus.WAITING)
                .build();

        SalesDeal deal = org.mockito.Mockito.mock(SalesDeal.class);
        when(deal.getCurrentStage()).thenReturn(DealStage.PENDING_CLIENT);

        SalesDealLog latestLog = org.mockito.Mockito.mock(SalesDealLog.class);
        when(latestLog.getDeal()).thenReturn(deal);
        when(salesDealLogRepository.findTopByDocTypeAndRefIdOrderByActionAtDescIdDesc(DealType.QUO, 100L))
                .thenReturn(Optional.of(latestLog));

        approvalDealLogWriter.writeDecision(
                request,
                step,
                DecisionType.APPROVE,
                "WAITING_ADMIN",
                "WAITING_CLIENT",
                DealStage.PENDING_ADMIN.name(),
                DealStage.PENDING_CLIENT.name(),
                null,
                ActorType.ADMIN,
                99L
        );

        ArgumentCaptor<DealStage> fromStageCaptor = ArgumentCaptor.forClass(DealStage.class);
        verify(dealPipelineFacade).recordAndSync(
                any(SalesDeal.class),
                any(DealType.class),
                any(Long.class),
                any(String.class),
                fromStageCaptor.capture(),
                any(DealStage.class),
                any(String.class),
                any(String.class),
                any(ActionType.class),
                any(),
                any(ActorType.class),
                any(Long.class),
                any(),
                any(java.util.List.class)
        );

        assertThat(fromStageCaptor.getValue()).isEqualTo(DealStage.PENDING_CLIENT);
    }
}
