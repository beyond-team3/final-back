package com.monsoon.seedflowplus.domain.sales.contract.service;

import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

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
    @DisplayName("계약 상태 변경 로직 검증: 벌크 업데이트 메서드 호출 확인 (가시화 대체)")
    void syncStatus_ShouldCallBulkUpdateMethods() {
        // given
        LocalDate today = LocalDate.now();
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
}
