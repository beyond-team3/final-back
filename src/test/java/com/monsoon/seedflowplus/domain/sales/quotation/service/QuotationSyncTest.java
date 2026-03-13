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
    @DisplayName("서비스 계층 상태 전이 및 복구 검증: 벌크 업데이트 메서드 호출 확인")
    void syncStatus_ShouldCallBulkUpdateMethods() {
        // given
        LocalDate today = LocalDate.now();
        when(quotationRepository.updateStatusForExpiration(any(), any(), any())).thenReturn(1);
        when(quotationRequestRepository.recoverStatusByExpiredQuotation(any(), any(), any())).thenReturn(1);

        System.out.println("\n[견적서 테스트] 벌크 업데이트 및 RFQ 복구 검증 시작");

        // when
        quotationService.syncQuotationStatuses();

        // then
        verify(quotationRepository, times(1)).updateStatusForExpiration(
                eq(QuotationStatus.WAITING_ADMIN), eq(QuotationStatus.EXPIRED), eq(today));
        
        // RFQ 상태 복구 로직이 주석 처리되었으므로 호출되지 않아야 함
        verify(quotationRequestRepository, never()).recoverStatusByExpiredQuotation(any(), any(), any());

        System.out.println(">>> Repository 벌크 업데이트 메서드(Quotation Expiration) 호출 확인 완료 (RFQ 복구 생략)");
        System.out.println("[견적서 테스트] 완료");
    }
}
