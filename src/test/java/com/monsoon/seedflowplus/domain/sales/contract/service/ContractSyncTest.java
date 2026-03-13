package com.monsoon.seedflowplus.domain.sales.contract.service;

import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealRepository;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogWriteService;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractSyncTest {

    @InjectMocks
    private ContractService contractService;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private SalesDealRepository salesDealRepository;

    @Mock
    private DealLogWriteService dealLogWriteService;

    @Test
    @DisplayName("계약 상태 변경 로직 검증: 벌크 업데이트 메서드 호출 확인 (가시화 대체)")
    void syncStatus_ShouldCallBulkUpdateMethods() {
        // given
        LocalDate today = LocalDate.now();
        when(contractRepository.findByStatusAndEndDateLessThan(ContractStatus.ACTIVE_CONTRACT, today))
                .thenReturn(List.of());
        when(contractRepository.updateStatusForActivation(any(), any(), any())).thenReturn(1);
        when(contractRepository.updateStatusForExpiration(any(), any(), any())).thenReturn(1);

        System.out.println("\n[계약서 테스트] 벌크 업데이트 검증 시작");

        // when
        contractService.syncContractStatuses();

        // then
        verify(contractRepository, times(1)).updateStatusForActivation(
                eq(ContractStatus.COMPLETED), eq(ContractStatus.ACTIVE_CONTRACT), eq(today));
        verify(contractRepository, times(1)).updateStatusForExpiration(
                eq(ContractStatus.ACTIVE_CONTRACT), eq(ContractStatus.EXPIRED), eq(today));

        System.out.println(">>> Repository 벌크 업데이트 메서드(Activation, Expiration) 호출 확인 완료");
        System.out.println("[계약서 테스트] 완료");
    }

    @Test
    @DisplayName("계약 만료 동기화 시 연결된 deal을 닫는다")
    void syncStatus_ShouldCloseExpiredDeals() {
        LocalDate today = LocalDate.now();
        SalesDeal deal = org.mockito.Mockito.mock(SalesDeal.class);
        when(deal.getId()).thenReturn(20L);

        ContractHeader contract = org.mockito.Mockito.mock(ContractHeader.class);
        when(contract.getId()).thenReturn(201L);
        when(contract.getContractCode()).thenReturn("CNT-201");
        when(contract.getDeal()).thenReturn(deal);

        SalesDeal managedDeal = SalesDeal.builder()
                .client(org.mockito.Mockito.mock(com.monsoon.seedflowplus.domain.account.entity.Client.class))
                .ownerEmp(org.mockito.Mockito.mock(com.monsoon.seedflowplus.domain.account.entity.Employee.class))
                .currentStage(DealStage.CONFIRMED)
                .currentStatus(ContractStatus.ACTIVE_CONTRACT.name())
                .latestDocType(DealType.CNT)
                .latestRefId(1L)
                .latestTargetCode("CNT-1")
                .lastActivityAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(managedDeal, "id", 20L);

        when(contractRepository.findByStatusAndEndDateLessThan(ContractStatus.ACTIVE_CONTRACT, today))
                .thenReturn(List.of(contract));
        when(contractRepository.updateStatusForActivation(any(), any(), any())).thenReturn(0);
        when(contractRepository.updateStatusForExpiration(any(), any(), any())).thenReturn(1);
        when(salesDealRepository.findAllById(java.util.Set.of(20L))).thenReturn(List.of(managedDeal));

        contractService.syncContractStatuses();

        org.assertj.core.api.Assertions.assertThat(managedDeal.getClosedAt()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(managedDeal.getCurrentStatus()).isEqualTo(ContractStatus.EXPIRED.name());
        verify(dealLogWriteService).write(
                eq(managedDeal),
                eq(DealType.CNT),
                eq(201L),
                eq("CNT-201"),
                eq(DealStage.CONFIRMED),
                eq(DealStage.EXPIRED),
                eq(ContractStatus.ACTIVE_CONTRACT.name()),
                eq(ContractStatus.EXPIRED.name()),
                eq(com.monsoon.seedflowplus.domain.deal.common.ActionType.EXPIRE),
                any(),
                eq(com.monsoon.seedflowplus.domain.deal.common.ActorType.SYSTEM),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                any(java.util.List.class)
        );
    }
}
