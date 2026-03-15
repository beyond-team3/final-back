package com.monsoon.seedflowplus.domain.deal.log.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.log.entity.SalesDealLog;
import com.monsoon.seedflowplus.domain.deal.log.policy.DealLogPolicyValidator;
import com.monsoon.seedflowplus.domain.deal.log.repository.DealLogDetailRepository;
import com.monsoon.seedflowplus.domain.deal.log.repository.SalesDealLogRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class DealLogWriteServiceTest {

    @Mock
    private SalesDealLogRepository salesDealLogRepository;

    @Mock
    private DealLogDetailRepository dealLogDetailRepository;

    private DealLogWriteService dealLogWriteService;

    @BeforeEach
    void setUp() {
        dealLogWriteService = new DealLogWriteService(
                salesDealLogRepository,
                dealLogDetailRepository,
                new DealLogPolicyValidator(),
                new ObjectMapper()
        );
        when(salesDealLogRepository.save(any(SalesDealLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void writeConvertPairShouldSaveTwoLogs() {
        // 정책: CONVERT는 예외적으로 CONVERT + CREATE 로그 2건이 저장되어야 한다.
        SalesDeal deal = testDeal();

        DealLogWriteService.ConvertLogRequest original = new DealLogWriteService.ConvertLogRequest(
                deal,
                DealType.QUO,
                10L,
                "QUO-20260305-10",
                DealStage.PENDING_CLIENT,
                "FINAL_APPROVED",
                "COMPLETED",
                LocalDateTime.now(),
                ActorType.SALES_REP,
                100L,
                null,
                null
        );

        DealLogWriteService.ConvertLogRequest created = new DealLogWriteService.ConvertLogRequest(
                deal,
                DealType.CNT,
                11L,
                "CNT-20260305-11",
                DealStage.APPROVED,
                "WAITING_ADMIN",
                "WAITING_ADMIN",
                original.actionAt(),
                ActorType.SALES_REP,
                100L,
                null,
                null
        );

        dealLogWriteService.writeConvertPair(original, created);

        ArgumentCaptor<SalesDealLog> captor = ArgumentCaptor.forClass(SalesDealLog.class);
        verify(salesDealLogRepository, times(2)).save(captor.capture());
        assertEquals(ActionType.CONVERT, captor.getAllValues().get(0).getActionType());
        assertEquals("QUO-20260305-10", captor.getAllValues().get(0).getTargetCode());
        assertEquals(ActionType.CREATE, captor.getAllValues().get(1).getActionType());
        assertEquals("CNT-20260305-11", captor.getAllValues().get(1).getTargetCode());
    }

    @Test
    void writeShouldSaveOneLogForNormalStatusChange() {
        // 정책: 일반 상태 변경은 로그 1건만 저장되어야 한다.
        SalesDeal deal = testDeal();

        dealLogWriteService.write(
                deal,
                DealType.ORD,
                99L,
                "ORD-20260305-99",
                DealStage.IN_PROGRESS,
                DealStage.CONFIRMED,
                "PENDING",
                "CONFIRMED",
                ActionType.CONFIRM,
                null,
                ActorType.SALES_REP,
                101L
        );

        ArgumentCaptor<SalesDealLog> captor = ArgumentCaptor.forClass(SalesDealLog.class);
        verify(salesDealLogRepository, times(1)).save(captor.capture());
        assertEquals(ActionType.CONFIRM, captor.getValue().getActionType());
        assertEquals("ORD-20260305-99", captor.getValue().getTargetCode());
    }

    private SalesDeal testDeal() {
        return SalesDeal.builder()
                .client(Mockito.mock(Client.class))
                .ownerEmp(Mockito.mock(Employee.class))
                .currentStage(DealStage.PENDING_ADMIN)
                .currentStatus("WAITING_ADMIN")
                .latestDocType(DealType.QUO)
                .latestRefId(1L)
                .latestTargetCode("QUO-20260305-1")
                .lastActivityAt(LocalDateTime.now())
                .closedAt(null)
                .summaryMemo(null)
                .build();
    }
}
