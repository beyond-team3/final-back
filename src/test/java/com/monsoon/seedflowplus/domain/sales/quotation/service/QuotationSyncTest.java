package com.monsoon.seedflowplus.domain.sales.quotation.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationStatus;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.core.repository.SalesDealRepository;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogWriteService;
import com.monsoon.seedflowplus.domain.sales.quotation.repository.QuotationRepository;
import com.monsoon.seedflowplus.domain.sales.quotation.entity.QuotationHeader;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestStatus;
import com.monsoon.seedflowplus.domain.sales.request.repository.QuotationRequestRepository;
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
class QuotationSyncTest {

    @InjectMocks
    private QuotationService quotationService;

    @Mock
    private QuotationRepository quotationRepository;

    @Mock
    private QuotationRequestRepository quotationRequestRepository;

    @Mock
    private SalesDealRepository salesDealRepository;

    @Mock
    private DealLogWriteService dealLogWriteService;

    @Test
    @DisplayName("견적서 만료 시 RFQ 미복구 및 상태 전이 검증")
    void syncStatus_NoRecovery_ShouldExpireQuotation() {
        // given
        LocalDate today = LocalDate.now();
        when(quotationRepository.findByStatusAndExpiredDateLessThanEqual(QuotationStatus.WAITING_ADMIN, today))
                .thenReturn(List.of());
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

    @Test
    @DisplayName("견적 만료 동기화 시 연결된 deal을 닫는다")
    void syncStatus_ShouldCloseExpiredDeals() {
        LocalDate today = LocalDate.now();
        SalesDeal deal = org.mockito.Mockito.mock(SalesDeal.class);
        when(deal.getId()).thenReturn(10L);

        QuotationHeader quotation = org.mockito.Mockito.mock(QuotationHeader.class);
        when(quotation.getId()).thenReturn(101L);
        when(quotation.getQuotationCode()).thenReturn("QUO-101");
        when(quotation.getDeal()).thenReturn(deal);

        SalesDeal managedDeal = SalesDeal.builder()
                .client(org.mockito.Mockito.mock(com.monsoon.seedflowplus.domain.account.entity.Client.class))
                .ownerEmp(org.mockito.Mockito.mock(com.monsoon.seedflowplus.domain.account.entity.Employee.class))
                .currentStage(DealStage.PENDING_ADMIN)
                .currentStatus(QuotationStatus.WAITING_ADMIN.name())
                .latestDocType(DealType.QUO)
                .latestRefId(1L)
                .latestTargetCode("QUO-1")
                .lastActivityAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(managedDeal, "id", 10L);

        when(quotationRepository.findByStatusAndExpiredDateLessThanEqual(QuotationStatus.WAITING_ADMIN, today))
                .thenReturn(List.of(quotation));
        when(quotationRepository.updateStatusForExpiration(any(), any(), any())).thenReturn(1);
        when(quotationRequestRepository.recoverStatusByExpiredQuotation(any(), any(), any())).thenReturn(0);
        when(salesDealRepository.findAllById(java.util.Set.of(10L))).thenReturn(List.of(managedDeal));

        quotationService.syncQuotationStatuses();

        assertThat(managedDeal.getClosedAt()).isNotNull();
        assertThat(managedDeal.getCurrentStatus()).isEqualTo(QuotationStatus.EXPIRED.name());
        verify(dealLogWriteService).write(
                eq(managedDeal),
                eq(DealType.QUO),
                eq(101L),
                eq("QUO-101"),
                eq(DealStage.PENDING_ADMIN),
                eq(DealStage.EXPIRED),
                eq(QuotationStatus.WAITING_ADMIN.name()),
                eq(QuotationStatus.EXPIRED.name()),
                eq(com.monsoon.seedflowplus.domain.deal.common.ActionType.EXPIRE),
                any(),
                eq(com.monsoon.seedflowplus.domain.deal.common.ActorType.SYSTEM),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                any(java.util.List.class)
        );
    }
}
