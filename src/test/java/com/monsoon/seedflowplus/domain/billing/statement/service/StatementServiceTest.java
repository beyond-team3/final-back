package com.monsoon.seedflowplus.domain.billing.statement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.billing.invoice.repository.InvoiceStatementRepository;
import com.monsoon.seedflowplus.domain.billing.statement.entity.Statement;
import com.monsoon.seedflowplus.domain.billing.statement.repository.StatementRepository;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogQueryService;
import com.monsoon.seedflowplus.domain.deal.log.service.DealPipelineFacade;
import com.monsoon.seedflowplus.domain.notification.event.NotificationEventPublisher;
import com.monsoon.seedflowplus.domain.notification.event.StatementIssuedEvent;
import com.monsoon.seedflowplus.domain.sales.contract.entity.BillingCycle;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractDetail;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderDetail;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderHeader;
import com.monsoon.seedflowplus.domain.sales.order.repository.OrderDetailRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.math.BigDecimal;
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
class StatementServiceTest {

    @Mock
    private StatementRepository statementRepository;
    @Mock
    private InvoiceStatementRepository invoiceStatementRepository;
    @Mock
    private DealPipelineFacade dealPipelineFacade;
    @Mock
    private DealLogQueryService dealLogQueryService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationEventPublisher notificationEventPublisher;
    @Mock
    private OrderDetailRepository orderDetailRepository;

    private StatementService statementService;

    @BeforeEach
    void setUp() {
        statementService = new StatementService(
                statementRepository,
                invoiceStatementRepository,
                dealPipelineFacade,
                dealLogQueryService,
                userRepository,
                notificationEventPublisher,
                orderDetailRepository
        );
    }

    @Test
    void createStatementPublishesEventsToSalesRepDealOwnerAndClient() {
        Client client = org.mockito.Mockito.mock(Client.class);
        when(client.getId()).thenReturn(7L);

        Employee employee = org.mockito.Mockito.mock(Employee.class);
        when(employee.getId()).thenReturn(12L);

        SalesDeal deal = org.mockito.Mockito.mock(SalesDeal.class);
        Employee ownerEmployee = org.mockito.Mockito.mock(Employee.class);
        when(ownerEmployee.getId()).thenReturn(13L);
        when(deal.getOwnerEmp()).thenReturn(ownerEmployee);

        ContractHeader contract = org.mockito.Mockito.mock(ContractHeader.class);

        OrderHeader orderHeader = OrderHeader.create(contract, client, deal, employee, "ORD-20260312-001");
        ReflectionTestUtils.setField(orderHeader, "id", 41L);
        orderHeader.updateTotalAmount(new BigDecimal("150000"));

        User salesUser = org.mockito.Mockito.mock(User.class);
        when(salesUser.getId()).thenReturn(1000L);
        when(salesUser.getEmployee()).thenReturn(employee);
        User ownerUser = org.mockito.Mockito.mock(User.class);
        when(ownerUser.getId()).thenReturn(1500L);
        when(ownerUser.getEmployee()).thenReturn(ownerEmployee);
        User clientUser = org.mockito.Mockito.mock(User.class);
        when(clientUser.getId()).thenReturn(2000L);
        when(clientUser.getClient()).thenReturn(client);

        when(statementRepository.findByOrderHeader_Id(41L)).thenReturn(Optional.empty());
        when(statementRepository.findMaxSuffixByPrefix(any())).thenReturn(Optional.of(0));
        when(statementRepository.saveAndFlush(any(Statement.class))).thenAnswer(invocation -> {
            Statement statement = invocation.getArgument(0);
            ReflectionTestUtils.setField(statement, "id", 51L);
            ReflectionTestUtils.setField(statement, "createdAt", LocalDateTime.of(2026, 3, 12, 15, 0));
            return statement;
        });
        when(userRepository.findAllByEmployeeIdIn(List.of(12L, 13L))).thenReturn(List.of(salesUser, ownerUser));
        when(userRepository.findAllByClientIdIn(List.of(7L))).thenReturn(List.of(clientUser));

        statementService.createStatement(orderHeader, ActorType.SALES_REP, 12L);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(notificationEventPublisher, org.mockito.Mockito.times(3)).publishAfterCommit(eventCaptor.capture());
        assertEquals(3, eventCaptor.getAllValues().size());
        StatementIssuedEvent firstEvent = (StatementIssuedEvent) eventCaptor.getAllValues().get(0);
        StatementIssuedEvent secondEvent = (StatementIssuedEvent) eventCaptor.getAllValues().get(1);
        StatementIssuedEvent thirdEvent = (StatementIssuedEvent) eventCaptor.getAllValues().get(2);
        assertEquals(51L, firstEvent.statementId());
        assertEquals("ORD-20260312-001", firstEvent.orderCode());
        assertEquals(51L, secondEvent.statementId());
        assertEquals(51L, thirdEvent.statementId());
        assertEquals(firstEvent.occurredAt(), secondEvent.occurredAt());
        assertEquals(secondEvent.occurredAt(), thirdEvent.occurredAt());
    }

    @Test
    void getStatementIncludesDocumentContextAndItems() {
        Client client = org.mockito.Mockito.mock(Client.class);
        when(client.getId()).thenReturn(7L);
        when(client.getClientName()).thenReturn("테스트 거래처");

        Employee employee = org.mockito.Mockito.mock(Employee.class);
        when(employee.getId()).thenReturn(12L);
        when(employee.getEmployeeName()).thenReturn("담당 영업");

        SalesDeal deal = org.mockito.Mockito.mock(SalesDeal.class);
        when(deal.getId()).thenReturn(55L);

        ContractHeader contract = ContractHeader.create(
                "CNT-1",
                null,
                client,
                deal,
                employee,
                BigDecimal.valueOf(10000),
                java.time.LocalDate.of(2026, 3, 1),
                java.time.LocalDate.of(2026, 6, 30),
                BillingCycle.MONTHLY,
                null,
                null
        );
        ReflectionTestUtils.setField(contract, "id", 91L);

        OrderHeader orderHeader = OrderHeader.create(contract, client, deal, employee, "ORD-1");
        ReflectionTestUtils.setField(orderHeader, "id", 41L);
        ReflectionTestUtils.setField(orderHeader, "deliveryDate", java.time.LocalDate.of(2026, 3, 20));
        orderHeader.updateTotalAmount(new BigDecimal("150000"));

        Statement statement = Statement.create(orderHeader, deal, new BigDecimal("150000"), "STMT-1");
        ReflectionTestUtils.setField(statement, "id", 51L);
        ReflectionTestUtils.setField(statement, "createdAt", LocalDateTime.of(2026, 3, 12, 15, 0));

        ContractDetail contractDetail = new ContractDetail(null, "토마토", "VEG", 10, "EA", BigDecimal.valueOf(1000), BigDecimal.valueOf(10000));
        ReflectionTestUtils.setField(contractDetail, "id", 77L);
        OrderDetail orderDetail = OrderDetail.create(orderHeader, contractDetail, 5L, "홍길동", "010-1111-2222", "서울", "101호", "문앞");
        ReflectionTestUtils.setField(orderDetail, "id", 88L);

        User principalUser = User.builder()
                .loginId("sales")
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.SALES_REP)
                .employee(employee)
                .build();
        ReflectionTestUtils.setField(principalUser, "id", 99L);
        CustomUserDetails principal = new CustomUserDetails(principalUser);
        when(statementRepository.findById(51L)).thenReturn(Optional.of(statement));
        when(invoiceStatementRepository.findTopByStatementIdAndIncludedTrueOrderByIdDesc(51L)).thenReturn(Optional.empty());
        when(dealLogQueryService.getRecentDocumentLogs(55L, com.monsoon.seedflowplus.domain.deal.common.DealType.STMT, 51L)).thenReturn(List.of());
        when(orderDetailRepository.findByOrderHeader_Id(41L)).thenReturn(List.of(orderDetail));

        com.monsoon.seedflowplus.domain.billing.statement.dto.response.StatementResponse response =
                statementService.getStatement(51L, principal);

        assertEquals(91L, response.getContractId());
        assertEquals("CNT-1", response.getContractCode());
        assertEquals("테스트 거래처", response.getClientName());
        assertEquals(BillingCycle.MONTHLY, response.getBillingCycle());
        assertEquals(1, response.getItems().size());
        assertEquals("토마토", response.getItems().get(0).getProductName());
        assertEquals("홍길동", response.getShippingName());
    }
}
