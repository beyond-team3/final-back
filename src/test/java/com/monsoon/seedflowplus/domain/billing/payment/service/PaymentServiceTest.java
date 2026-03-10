package com.monsoon.seedflowplus.domain.billing.payment.service;

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
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.Invoice;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatus;
import com.monsoon.seedflowplus.domain.billing.invoice.repository.InvoiceRepository;
import com.monsoon.seedflowplus.domain.billing.payment.dto.request.PaymentCreateRequest;
import com.monsoon.seedflowplus.domain.billing.payment.dto.response.PaymentResponse;
import com.monsoon.seedflowplus.domain.billing.payment.entity.Payment;
import com.monsoon.seedflowplus.domain.billing.payment.entity.PaymentMethod;
import com.monsoon.seedflowplus.domain.billing.payment.repository.PaymentRepository;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogQueryService;
import com.monsoon.seedflowplus.domain.deal.log.service.DealPipelineFacade;
import com.monsoon.seedflowplus.domain.schedule.dto.command.DealScheduleUpsertCommand;
import com.monsoon.seedflowplus.domain.schedule.sync.DealScheduleSyncService;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DealPipelineFacade dealPipelineFacade;
    @Mock
    private DealLogQueryService dealLogQueryService;
    @Mock
    private DealScheduleSyncService dealScheduleSyncService;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                invoiceRepository,
                clientRepository,
                userRepository,
                dealPipelineFacade,
                dealLogQueryService,
                dealScheduleSyncService
        );
    }

    @Test
    void processPaymentShouldUpsertPaymentReceivedSchedule() {
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

        SalesDeal deal = org.mockito.Mockito.mock(SalesDeal.class);
        when(deal.getId()).thenReturn(55L);
        when(deal.getOwnerEmp()).thenReturn(ownerEmployee);

        Invoice invoice = Invoice.create(10L, client, deal, ownerEmployee, LocalDate.of(2026, 3, 15),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), "INV-20260310-001", null);
        ReflectionTestUtils.setField(invoice, "id", 41L);
        invoice.publish();

        Payment payment = Payment.create(invoice, client, deal, PaymentMethod.TRANSFER, "PAY-20260310-001");
        ReflectionTestUtils.setField(payment, "id", 51L);
        ReflectionTestUtils.setField(payment, "createdAt", LocalDateTime.of(2026, 3, 10, 9, 0));

        User assigneeUser = User.builder()
                .loginId("sales")
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.SALES_REP)
                .employee(ownerEmployee)
                .build();
        ReflectionTestUtils.setField(assigneeUser, "id", 99L);

        PaymentCreateRequest request = new PaymentCreateRequest();
        ReflectionTestUtils.setField(request, "invoiceId", 41L);
        ReflectionTestUtils.setField(request, "paymentMethod", PaymentMethod.TRANSFER);

        when(invoiceRepository.findById(41L)).thenReturn(Optional.of(invoice));
        when(clientRepository.findById(7L)).thenReturn(Optional.of(client));
        when(paymentRepository.findMaxSuffixByPrefix(any())).thenReturn(Optional.of(0));
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 51L);
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.of(2026, 3, 10, 9, 0));
            return saved;
        });
        when(userRepository.findByEmployeeId(12L)).thenReturn(Optional.of(assigneeUser));
        when(dealLogQueryService.getRecentDocumentLogs(any(), any(), any())).thenReturn(List.of());

        PaymentResponse response = paymentService.processPayment(request, 7L);

        ArgumentCaptor<DealScheduleUpsertCommand> commandCaptor = ArgumentCaptor.forClass(DealScheduleUpsertCommand.class);
        verify(dealScheduleSyncService, times(1)).upsertFromEvent(commandCaptor.capture());
        DealScheduleUpsertCommand command = commandCaptor.getValue();
        assertEquals("PAY_51_PAYMENT_RECEIVED", command.externalKey());
        assertEquals("결제 완료: 테스트 거래처", command.title());
        assertEquals(LocalDateTime.of(2026, 3, 10, 0, 0), command.startAt());
        assertEquals(LocalDateTime.of(2026, 3, 11, 0, 0), command.endAt());
        assertEquals(51L, response.getPaymentId());
        assertEquals(com.monsoon.seedflowplus.domain.billing.payment.entity.PaymentStatus.COMPLETED, response.getStatus());
    }
}
