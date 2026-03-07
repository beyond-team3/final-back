package com.monsoon.seedflowplus.domain.sales.contract.service;

import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractSyncTest {

    @InjectMocks
    private ContractService contractService;

    @Mock
    private ContractRepository contractRepository;

    @Test
    @DisplayName("계약 활성화 시각화 테스트: COMPLETED -> ACTIVE_CONTRACT")
    void syncStatus_VisualizeContractActivation() {
        LocalDate today = LocalDate.now();
        ContractHeader contract = ContractHeader.create(
                "TEST-CNT", null, mock(com.monsoon.seedflowplus.domain.account.entity.Client.class),
                mock(com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal.class), null,
                java.math.BigDecimal.ZERO, today, today.plusDays(10),
                com.monsoon.seedflowplus.domain.sales.contract.entity.BillingCycle.MONTHLY, null, null
        );
        contract.updateStatus(ContractStatus.COMPLETED);

        System.out.println("\n[계약서 테스트] 활성화 검증 시작");
        System.out.println(">>> 변경 전 상태: " + contract.getStatus() + " (시작일: " + today + ")");

        when(contractRepository.findByStatusAndStartDateLessThanEqual(eq(ContractStatus.COMPLETED), any(LocalDate.class)))
                .thenReturn(List.of(contract));

        contractService.syncContractStatuses();

        System.out.println(">>> 변경 후 상태: " + contract.getStatus());
        System.out.println("[계약서 테스트] 완료");

        org.junit.jupiter.api.Assertions.assertEquals(ContractStatus.ACTIVE_CONTRACT, contract.getStatus());
    }

    @Test
    @DisplayName("계약 만료 시각화 테스트: ACTIVE_CONTRACT -> EXPIRED")
    void syncStatus_VisualizeContractExpiration() {
        LocalDate today = LocalDate.now();
        ContractHeader contract = ContractHeader.create(
                "TEST-CNT-EX", null, mock(com.monsoon.seedflowplus.domain.account.entity.Client.class),
                mock(com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal.class), null,
                java.math.BigDecimal.ZERO, today.minusDays(10), today.minusDays(1),
                com.monsoon.seedflowplus.domain.sales.contract.entity.BillingCycle.MONTHLY, null, null
        );
        contract.updateStatus(ContractStatus.ACTIVE_CONTRACT);

        System.out.println("\n[계약서 테스트] 만료 검증 시작");
        System.out.println(">>> 변경 전 상태: " + contract.getStatus() + " (종료일: " + today.minusDays(1) + ")");

        when(contractRepository.findByStatusAndEndDateLessThan(eq(ContractStatus.ACTIVE_CONTRACT), any(LocalDate.class)))
                .thenReturn(List.of(contract));

        contractService.syncContractStatuses();

        System.out.println(">>> 변경 후 상태: " + contract.getStatus());
        System.out.println("[계약서 테스트] 완료");

        org.junit.jupiter.api.Assertions.assertEquals(ContractStatus.EXPIRED, contract.getStatus());
    }
}
