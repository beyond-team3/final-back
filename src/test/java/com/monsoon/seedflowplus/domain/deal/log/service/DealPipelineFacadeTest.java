package com.monsoon.seedflowplus.domain.deal.log.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
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
import com.monsoon.seedflowplus.domain.notification.event.NotificationEventPublisher;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class DealPipelineFacadeTest {

    @Mock
    private SalesDealLogRepository salesDealLogRepository;

    @Mock
    private DealLogDetailRepository dealLogDetailRepository;

    @Mock
    private DocStatusTransitionValidator docStatusTransitionValidator;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @Mock
    private UserRepository userRepository;

    private DealPipelineFacade dealPipelineFacade;

    @BeforeEach
    void setUp() {
        DealLogWriteService dealLogWriteService = new DealLogWriteService(
                salesDealLogRepository,
                dealLogDetailRepository,
                new DealLogPolicyValidator(),
                new ObjectMapper()
        );
        dealPipelineFacade = new DealPipelineFacade(
                dealLogWriteService,
                docStatusTransitionValidator,
                notificationEventPublisher,
                userRepository
        );
    }

    @Test
    void recordAndSyncShouldNotSaveLogWhenTransitionValidationFails() {
        // 정책: 상태 전이 검증 실패 시 예외를 던지고 로그(save)는 0회여야 한다.
        SalesDeal deal = testDeal();
        doThrow(new CoreException(ErrorType.INVALID_DOC_STATUS_TRANSITION))
                .when(docStatusTransitionValidator)
                .validateOrThrow(DealType.ORD, "PENDING", ActionType.CONFIRM, "CONFIRMED");

        CoreException ex = assertThrows(
                CoreException.class,
                () -> dealPipelineFacade.recordAndSync(
                        deal,
                        DealType.ORD,
                        44L,
                        "ORD-20260305-44",
                        DealStage.IN_PROGRESS,
                        DealStage.CONFIRMED,
                        "PENDING",
                        "CONFIRMED",
                        ActionType.CONFIRM,
                        null,
                        ActorType.SALES_REP,
                        123L,
                        null,
                        List.of()
                )
        );
        org.junit.jupiter.api.Assertions.assertEquals(ErrorType.INVALID_DOC_STATUS_TRANSITION, ex.getErrorType());

        verify(salesDealLogRepository, never()).save(any(SalesDealLog.class));
    }

    private SalesDeal testDeal() {
        return SalesDeal.builder()
                .client(Mockito.mock(Client.class))
                .ownerEmp(Mockito.mock(Employee.class))
                .currentStage(DealStage.IN_PROGRESS)
                .currentStatus("PENDING")
                .latestDocType(DealType.ORD)
                .latestRefId(1L)
                .latestTargetCode("ORD-20260305-1")
                .lastActivityAt(LocalDateTime.now())
                .closedAt(null)
                .summaryMemo(null)
                .build();
    }
}
