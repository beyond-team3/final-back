package com.monsoon.seedflowplus.domain.sales.quotation.service;

import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.sales.quotation.repository.QuotationRepository;
import com.monsoon.seedflowplus.domain.sales.request.repository.QuotationRequestRepository;
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
class QuotationSyncTest {

    @InjectMocks
    private QuotationService quotationService;

    @Mock
    private QuotationRepository quotationRepository;

    @Mock
    private QuotationRequestRepository quotationRequestRepository;

    @Test
    @DisplayName("견적서 만료 시 RFQ 미복구 및 상태 전이 검증")
    void syncStatus_NoRecovery_ShouldExpireQuotation() {
        // given
        LocalDate today = LocalDate.now();
        when(quotationRepository.updateStatusForExpiration(any(), any(), any())).thenReturn(1);
        // RFQ 복구는 호출되지 않지만 stubbing은 유지 (필요 시)
        // when(quotationRequestRepository.recoverStatusByExpiredQuotation(any(), any(), any())).thenReturn(1);

        System.out.println("\n[견적서 테스트] 견적서 만료 처리 검증 시작 (RFQ 복구 미수행)");

        // when
        quotationService.syncQuotationStatuses();

        // then
        verify(quotationRepository, times(1)).updateStatusForExpiration(
                eq(QuotationStatus.WAITING_ADMIN), eq(QuotationStatus.EXPIRED), eq(today));
        
        // RFQ 상태 복구 로직이 주석 처리되었으므로 호출되지 않아야 함을 검증
        verify(quotationRequestRepository, never()).recoverStatusByExpiredQuotation(any(), any(), any());

        System.out.println(">>> 견적서 만료(Expiration) 처리 확인 완료 (RFQ 상태는 유지됨)");
        System.out.println("[견적서 테스트] 완료");
    }
}
