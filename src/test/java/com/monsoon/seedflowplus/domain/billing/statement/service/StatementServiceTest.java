package com.monsoon.seedflowplus.domain.billing.statement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
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
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderHeader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    private StatementService statementService;

    @BeforeEach
    void setUp() {
        statementService = new StatementService(
                statementRepository,
                invoiceStatementRepository,
                dealPipelineFacade,
                dealLogQueryService,
                userRepository,
                notificationEventPublisher
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
        User ownerUser = org.mockito.Mockito.mock(User.class);
        when(ownerUser.getId()).thenReturn(1500L);
        User clientUser = org.mockito.Mockito.mock(User.class);
        when(clientUser.getId()).thenReturn(2000L);

        when(statementRepository.findByOrderHeader_Id(41L)).thenReturn(Optional.empty());
        when(statementRepository.findMaxSuffixByPrefix(any())).thenReturn(Optional.of(0));
        when(statementRepository.saveAndFlush(any(Statement.class))).thenAnswer(invocation -> {
            Statement statement = invocation.getArgument(0);
            ReflectionTestUtils.setField(statement, "id", 51L);
            ReflectionTestUtils.setField(statement, "createdAt", LocalDateTime.of(2026, 3, 12, 15, 0));
            return statement;
        });
        when(userRepository.findByEmployeeId(12L)).thenReturn(Optional.of(salesUser));
        when(userRepository.findByEmployeeId(13L)).thenReturn(Optional.of(ownerUser));
        when(userRepository.findByClientId(7L)).thenReturn(Optional.of(clientUser));

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
    }
}
