package com.monsoon.seedflowplus.domain.sales.quotation.service;

import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationHeader;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.sales.quotation.repository.QuotationRepository;
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
class QuotationSyncTest {

    @InjectMocks
    private QuotationService quotationService;

    @Mock
    private QuotationRepository quotationRepository;

    @Test
    @DisplayName("견적서 만료 시각화 테스트: WAITING_ADMIN -> EXPIRED")
    void syncStatus_VisualizeQuotationExpiration() {
        QuotationHeader quotation = QuotationHeader.create(
                null, "TEST-QUO", mock(com.monsoon.seedflowplus.domain.account.entity.Client.class),
                mock(com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal.class), null,
                java.math.BigDecimal.ZERO, "MEMO");
        // 기본 상태 WAITING_ADMIN

        System.out.println("\n[견적서 테스트] 만료 검증 시작");
        System.out.println(">>> 변경 전 상태: " + quotation.getStatus());

        when(quotationRepository.findByStatusAndExpiredDateLessThanEqual(eq(QuotationStatus.WAITING_ADMIN),
                any(LocalDate.class)))
                .thenReturn(List.of(quotation));

        quotationService.syncQuotationStatuses();

        System.out.println(">>> 변경 후 상태: " + quotation.getStatus());
        System.out.println("[견적서 테스트] 완료");

        org.junit.jupiter.api.Assertions.assertEquals(QuotationStatus.EXPIRED, quotation.getStatus());
    }

    @Test
    @DisplayName("견적서 만료 시 RFQ 상태 복구 테스트: REVIEWING -> PENDING")
    void syncStatus_RecoverRfqStatus() {
        // 1. 견적 요청서(RFQ) 준비
        com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestHeader rfq = mock(
                com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestHeader.class);
        when(rfq.getStatus())
                .thenReturn(com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus.REVIEWING);

        // 2. 견적서 준비 (RFQ와 연결됨)
        QuotationHeader quotation = QuotationHeader.create(
                rfq, "TEST-QUO-RFQ", mock(com.monsoon.seedflowplus.domain.account.entity.Client.class),
                mock(com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal.class), null,
                java.math.BigDecimal.ZERO, "MEMO");

        System.out.println("\n[견적서 테스트] RFQ 상태 복구 검증 시작");

        when(quotationRepository.findByStatusAndExpiredDateLessThanEqual(eq(QuotationStatus.WAITING_ADMIN),
                any(LocalDate.class)))
                .thenReturn(List.of(quotation));

        // 3. 동기화 실행
        quotationService.syncQuotationStatuses();

        // 4. 검증
        org.junit.jupiter.api.Assertions.assertEquals(QuotationStatus.EXPIRED, quotation.getStatus());
        verify(rfq).updateStatus(com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus.PENDING);

        System.out.println(">>> 견적서 상태: " + quotation.getStatus());
        System.out.println(">>> RFQ 상태 복구 메서드 호출 확인 완료");
        System.out.println("[견적서 테스트] 완료");
    }
}
