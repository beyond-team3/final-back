package com.monsoon.seedflowplus.domain.billing.invoice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.EmployeeRepository;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.billing.invoice.dto.response.InvoicePublishResponse;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.Invoice;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatus;
import com.monsoon.seedflowplus.domain.billing.invoice.repository.InvoiceRepository;
import com.monsoon.seedflowplus.domain.billing.invoice.repository.InvoiceStatementRepository;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogQueryService;
import com.monsoon.seedflowplus.domain.deal.log.service.DealPipelineFacade;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import com.monsoon.seedflowplus.domain.schedule.dto.command.DealScheduleUpsertCommand;
import com.monsoon.seedflowplus.domain.schedule.entity.DealDocType;
import com.monsoon.seedflowplus.domain.schedule.sync.DealScheduleSyncService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InvoiceStatementRepository invoiceStatementRepository;
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DealPipelineFacade dealPipelineFacade;
    @Mock
    private DealLogQueryService dealLogQueryService;
    @Mock
    private DealScheduleSyncService dealScheduleSyncService;

    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        invoiceService = new InvoiceService(
                invoiceRepository,
                invoiceStatementRepository,
                contractRepository,
                clientRepository,
                employeeRepository,
                userRepository,
                dealPipelineFacade,
                dealLogQueryService,
                dealScheduleSyncService
        );
    }

    @Test
    void publishInvoiceShouldUpsertPaymentDueSchedule() {
        Client client = Client.builder()
                .clientCode("C-1")
                .clientName("테스트 거래처")
                .clientBrn("123-45-67890")
                .ceoName("대표")
                .companyPhone("02-0000-0000")
                .address("서울")
                .managerName("담당자")
                .managerPhone("010-0000-0000")
                .managerEmail("client@test.com")
                .build();
        ReflectionTestUtils.setField(client, "id", 7L);

        Employee ownerEmployee = Employee.builder()
                .employeeCode("EMP-1")
                .employeeName("담당 영업")
                .employeeEmail("owner@test.com")
                .employeePhone("010-1111-1111")
                .address("서울")
                .build();
        ReflectionTestUtils.setField(ownerEmployee, "id", 12L);

        Employee invoiceEmployee = Employee.builder()
                .employeeCode("EMP-2")
                .employeeName("발행자")
                .employeeEmail("invoice@test.com")
                .employeePhone("010-2222-2222")
                .address("서울")
                .build();

        SalesDeal deal = org.mockito.Mockito.mock(SalesDeal.class);
        when(deal.getId()).thenReturn(55L);
        when(deal.getOwnerEmp()).thenReturn(ownerEmployee);

        Invoice invoice = Invoice.create(10L, client, deal, invoiceEmployee, LocalDate.of(2026, 3, 15),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), "INV-20260310-001", null);
        ReflectionTestUtils.setField(invoice, "id", 41L);

        User assigneeUser = User.builder()
                .loginId("sales")
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.SALES_REP)
                .employee(ownerEmployee)
                .build();
        ReflectionTestUtils.setField(assigneeUser, "id", 99L);

        CustomUserDetails principal = org.mockito.Mockito.mock(CustomUserDetails.class);
        when(principal.getRole()).thenReturn(Role.ADMIN);
        when(principal.getEmployeeId()).thenReturn(100L);

        when(invoiceRepository.findById(41L)).thenReturn(Optional.of(invoice));
        when(userRepository.findByEmployeeId(12L)).thenReturn(Optional.of(assigneeUser));

        InvoicePublishResponse response = invoiceService.publishInvoice(41L, principal);

        ArgumentCaptor<DealScheduleUpsertCommand> commandCaptor = ArgumentCaptor.forClass(DealScheduleUpsertCommand.class);
        verify(dealScheduleSyncService, times(1)).upsertFromEvent(commandCaptor.capture());
        DealScheduleUpsertCommand command = commandCaptor.getValue();
        assertEquals("INV_41_PAYMENT_DUE", command.externalKey());
        assertEquals("결제 마감: 테스트 거래처", command.title());
        assertEquals(DealDocType.INV, command.docType());
        assertEquals(LocalDate.of(2026, 3, 15).atStartOfDay(), command.startAt());
        assertEquals(LocalDate.of(2026, 3, 16).atStartOfDay(), command.endAt());
        assertEquals(41L, response.getInvoiceId());
        assertEquals(InvoiceStatus.PUBLISHED, response.getStatus());
    }

    @Test
    void publishInvoiceShouldFallbackToPrincipalUserWhenOwnerUserMissing() {
        Client client = org.mockito.Mockito.mock(Client.class);
        when(client.getId()).thenReturn(7L);
        when(client.getClientName()).thenReturn("테스트 거래처");

        Employee ownerEmployee = org.mockito.Mockito.mock(Employee.class);
        when(ownerEmployee.getId()).thenReturn(12L);

        com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal deal = org.mockito.Mockito.mock(com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal.class);
        when(deal.getId()).thenReturn(88L);
        when(deal.getOwnerEmp()).thenReturn(ownerEmployee);

        Invoice invoice = Invoice.create(10L, client, deal, ownerEmployee, LocalDate.of(2026, 3, 15),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), "INV-20260315-001", null);
        ReflectionTestUtils.setField(invoice, "id", 41L);

        CustomUserDetails principal = org.mockito.Mockito.mock(CustomUserDetails.class);
        when(principal.getRole()).thenReturn(Role.ADMIN);
        when(principal.getEmployeeId()).thenReturn(100L);
        when(principal.getUserId()).thenReturn(7002L);

        when(invoiceRepository.findById(41L)).thenReturn(Optional.of(invoice));
        when(userRepository.findByEmployeeId(12L)).thenReturn(Optional.empty());

        invoiceService.publishInvoice(41L, principal);

        ArgumentCaptor<DealScheduleUpsertCommand> commandCaptor = ArgumentCaptor.forClass(DealScheduleUpsertCommand.class);
        verify(dealScheduleSyncService).upsertFromEvent(commandCaptor.capture());
        assertEquals(7002L, commandCaptor.getValue().assigneeUserId());
    }
}
