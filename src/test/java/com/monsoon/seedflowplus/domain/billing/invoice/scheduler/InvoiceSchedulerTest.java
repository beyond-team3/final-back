package com.monsoon.seedflowplus.domain.billing.invoice.scheduler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.billing.invoice.service.InvoiceService;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvoiceSchedulerTest {

    @Mock
    private ContractRepository contractHeaderRepository;

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private InvoiceScheduler invoiceScheduler;

    @Test
    void createInvoicesForDueDateUsesOnlyActiveContracts() {
        LocalDate today = LocalDate.of(2026, 3, 31);
        ContractHeader contract = org.mockito.Mockito.mock(ContractHeader.class);
        when(contract.getId()).thenReturn(10L);
        when(contract.getStartDate()).thenReturn(LocalDate.of(2026, 3, 1));
        when(contract.getEndDate()).thenReturn(LocalDate.of(2026, 6, 30));
        when(contract.getBillingCycle()).thenReturn(com.monsoon.seedflowplus.domain.sales.contract.entity.BillingCycle.MONTHLY);
        when(contractHeaderRepository.findAllByStatus(ContractStatus.ACTIVE_CONTRACT)).thenReturn(List.of(contract));

        org.mockito.MockedStatic<LocalDate> localDateMock = org.mockito.Mockito.mockStatic(LocalDate.class, org.mockito.Mockito.CALLS_REAL_METHODS);
        try {
            localDateMock.when(LocalDate::now).thenReturn(today);

            invoiceScheduler.autoCreateDraftInvoices();
        } finally {
            localDateMock.close();
        }

        verify(contractHeaderRepository).findAllByStatus(ContractStatus.ACTIVE_CONTRACT);
        verify(invoiceService).createDraftInvoice(contract);
    }

    @Test
    void createInvoicesForDueDateSkipsWhenNotMonthEnd() {
        LocalDate today = LocalDate.of(2026, 3, 30);

        org.mockito.MockedStatic<LocalDate> localDateMock = org.mockito.Mockito.mockStatic(LocalDate.class, org.mockito.Mockito.CALLS_REAL_METHODS);
        try {
            localDateMock.when(LocalDate::now).thenReturn(today);

            invoiceScheduler.autoCreateDraftInvoices();
        } finally {
            localDateMock.close();
        }

        verify(contractHeaderRepository, never()).findAllByStatus(ContractStatus.ACTIVE_CONTRACT);
        verify(invoiceService, never()).createDraftInvoice(org.mockito.ArgumentMatchers.any());
    }
}
