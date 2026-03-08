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
    @DisplayName("견적서 상태 변경 로직 검증: WAITING_ADMIN -> EXPIRED (Repository 데이터 존재 시)")
    void syncStatus_ShouldChangeToExpired_WhenRepositoryReturnsWaitingAdminQuotation() {
        // given
        QuotationHeader quotation = QuotationHeader.create(
                null, "TEST-QUO", mock(com.monsoon.seedflowplus.domain.account.entity.Client.class),
                mock(com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal.class), null,
                java.math.BigDecimal.ZERO, "MEMO");
        // 기본 상태 WAITING_ADMIN

        when(quotationRepository.findByStatusAndExpiredDateLessThanEqual(eq(QuotationStatus.WAITING_ADMIN),
                any(LocalDate.class)))
                .thenReturn(List.of(quotation));

        System.out.println("\n[견적서 테스트] 만료 검증 시작");
        System.out.println(">>> 변경 전 상태: " + quotation.getStatus());

        // when
        quotationService.syncQuotationStatuses();

        // then
        System.out.println(">>> 변경 후 상태: " + quotation.getStatus());
        System.out.println("[견적서 테스트] 완료");

        org.junit.jupiter.api.Assertions.assertEquals(QuotationStatus.EXPIRED, quotation.getStatus());
    }

    @Test
    @DisplayName("견적서 만료 시 연관된 견적 요청서 상태 복구 로직 검증: REVIEWING -> PENDING")
    void syncStatus_ShouldRecoverRfqStatus_WhenQuotationExpires() {
        // given
        // 1. 실제 견적 요청서 객체 생성 및 상태 설정 (상태 변화를 보기 위함)
        com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestHeader rfq = com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestHeader
                .create(
                        mock(com.monsoon.seedflowplus.domain.account.entity.Client.class),
                        "요구사항",
                        mock(com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal.class));
        rfq.updateStatus(com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus.REVIEWING);

        // 2. 견적서 준비 (견적 요청서와 연결됨)
        QuotationHeader quotation = QuotationHeader.create(
                rfq, "TEST-QUO-RFQ", mock(com.monsoon.seedflowplus.domain.account.entity.Client.class),
                mock(com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal.class), null,
                java.math.BigDecimal.ZERO, "MEMO");

        when(quotationRepository.findByStatusAndExpiredDateLessThanEqual(eq(QuotationStatus.WAITING_ADMIN),
                any(LocalDate.class)))
                .thenReturn(List.of(quotation));

        System.out.println("\n[견적서 테스트] 견적 요청서 상태 복구 검증 시작");
        System.out.println(">>> 변경 전 - 견적서 상태: " + quotation.getStatus());
        System.out.println(">>> 변경 전 - 견적 요청서 상태: " + rfq.getStatus());

        // when
        quotationService.syncQuotationStatuses();

        // then
        System.out.println(">>> 변경 후 - 견적서 상태: " + quotation.getStatus());
        System.out.println(">>> 변경 후 - 견적 요청서 상태: " + rfq.getStatus());
        System.out.println("[견적서 테스트] 완료");

        org.junit.jupiter.api.Assertions.assertEquals(QuotationStatus.EXPIRED, quotation.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(
                com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus.PENDING,
                rfq.getStatus());
    }
}
