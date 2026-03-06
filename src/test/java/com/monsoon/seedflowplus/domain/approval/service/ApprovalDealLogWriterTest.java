package com.monsoon.seedflowplus.domain.approval.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalDealLogWriterTest {

    @Mock
    private SalesDealLogRepository salesDealLogRepository;

    @Mock
    private DealLogWriteService dealLogWriteService;

    @Mock
    private DealPipelineFacade dealPipelineFacade;

    @InjectMocks
    private ApprovalDealLogWriter approvalDealLogWriter;

    @Test
    @DisplayName("writeDecision은 전달받은 fromStage와 deal currentStage가 다르면 예외를 던진다")
    void writeDecisionFailsWhenFromStageMismatchesDealStage() {
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
        when(deal.getId()).thenReturn(55L);

        SalesDealLog latestLog = org.mockito.Mockito.mock(SalesDealLog.class);
        when(latestLog.getDeal()).thenReturn(deal);
        when(salesDealLogRepository.findTopByDocTypeAndRefIdOrderByActionAtDescIdDesc(DealType.QUO, 100L))
                .thenReturn(Optional.of(latestLog));

        assertThatThrownBy(() -> approvalDealLogWriter.writeDecision(
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
        ))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.INVALID_DOC_STATUS_TRANSITION);

        verify(dealPipelineFacade, never()).recordAndSync(
                any(SalesDeal.class),
                any(DealType.class),
                any(Long.class),
                any(String.class),
                any(DealStage.class),
                any(DealStage.class),
                any(String.class),
                any(String.class),
                any(),
                any(),
                any(ActorType.class),
                any(Long.class),
                any(),
                any(java.util.List.class)
        );
    }

    @Test
    @DisplayName("writeSubmit은 deal snapshot 동기화 없이 로그만 기록한다")
    void writeSubmitUsesLogOnlyPath() {
        ApprovalRequest request = ApprovalRequest.builder()
                .dealType(DealType.QUO)
                .targetId(102L)
                .status(com.monsoon.seedflowplus.domain.approval.entity.ApprovalStatus.PENDING)
                .clientIdSnapshot(10L)
                .targetCodeSnapshot("Q-102")
                .build();
        ReflectionTestUtils.setField(request, "id", 3L);

        SalesDeal deal = org.mockito.Mockito.mock(SalesDeal.class);
        when(deal.getCurrentStage()).thenReturn(DealStage.CREATED);

        SalesDealLog latestLog = org.mockito.Mockito.mock(SalesDealLog.class);
        when(latestLog.getDeal()).thenReturn(deal);
        when(salesDealLogRepository.findTopByDocTypeAndRefIdOrderByActionAtDescIdDesc(DealType.QUO, 102L))
                .thenReturn(Optional.of(latestLog));

        approvalDealLogWriter.writeSubmit(request, ActorType.SALES_REP, 7L);

        verify(dealLogWriteService).write(
                eq(deal),
                eq(DealType.QUO),
                eq(102L),
                eq("Q-102"),
                eq(DealStage.CREATED),
                eq(DealStage.PENDING_ADMIN),
                eq("WAITING_ADMIN"),
                eq("WAITING_ADMIN"),
                eq(ActionType.SUBMIT),
                eq(null),
                eq(ActorType.SALES_REP),
                eq(7L),
                eq(null),
                any(java.util.List.class)
        );
        verify(dealPipelineFacade, never()).recordAndSync(
                any(SalesDeal.class),
                any(DealType.class),
                any(Long.class),
                any(String.class),
                any(DealStage.class),
                any(DealStage.class),
                any(String.class),
                any(String.class),
                any(),
                any(),
                any(ActorType.class),
                any(Long.class),
                any(),
                any(java.util.List.class)
        );
    }

    @Test
    @DisplayName("writeDecision은 enum에 없는 fromStage를 INVALID_DOC_STATUS_TRANSITION으로 변환한다")
    void writeDecisionFailsWhenFromStageIsInvalidEnum() {
        ApprovalRequest request = ApprovalRequest.builder()
                .dealType(DealType.QUO)
                .targetId(101L)
                .status(com.monsoon.seedflowplus.domain.approval.entity.ApprovalStatus.PENDING)
                .clientIdSnapshot(10L)
                .targetCodeSnapshot("Q-101")
                .build();
        ReflectionTestUtils.setField(request, "id", 2L);

        ApprovalStep step = ApprovalStep.builder()
                .approvalRequest(request)
                .stepOrder(1)
                .actorType(ActorType.ADMIN)
                .status(ApprovalStepStatus.WAITING)
                .build();

        SalesDeal deal = org.mockito.Mockito.mock(SalesDeal.class);
        when(deal.getCurrentStage()).thenReturn(DealStage.PENDING_ADMIN);

        SalesDealLog latestLog = org.mockito.Mockito.mock(SalesDealLog.class);
        when(latestLog.getDeal()).thenReturn(deal);
        when(salesDealLogRepository.findTopByDocTypeAndRefIdOrderByActionAtDescIdDesc(DealType.QUO, 101L))
                .thenReturn(Optional.of(latestLog));

        assertThatThrownBy(() -> approvalDealLogWriter.writeDecision(
                request,
                step,
                DecisionType.APPROVE,
                "WAITING_ADMIN",
                "WAITING_CLIENT",
                "NOT_A_STAGE",
                DealStage.PENDING_CLIENT.name(),
                null,
                ActorType.ADMIN,
                99L
        ))
                .isInstanceOf(CoreException.class)
                .extracting(ex -> ((CoreException) ex).getErrorType())
                .isEqualTo(ErrorType.INVALID_DOC_STATUS_TRANSITION);

        verify(dealPipelineFacade, never()).recordAndSync(
                any(SalesDeal.class),
                any(DealType.class),
                any(Long.class),
                any(String.class),
                any(DealStage.class),
                any(DealStage.class),
                any(String.class),
                any(String.class),
                any(),
                any(),
                any(ActorType.class),
                any(Long.class),
                any(),
                any(java.util.List.class)
        );
    }
}
