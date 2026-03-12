package com.monsoon.seedflowplus.domain.sales.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.EmployeeRepository;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.billing.statement.service.StatementService;
import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogQueryService;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogWriteService;
import com.monsoon.seedflowplus.domain.deal.log.service.DealPipelineFacade;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractDetail;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractDetailRepository;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import com.monsoon.seedflowplus.domain.sales.order.dto.request.OrderCreateRequest;
import com.monsoon.seedflowplus.domain.sales.order.dto.request.OrderDetailRequest;
import com.monsoon.seedflowplus.domain.sales.order.dto.response.OrderResponse;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderDetail;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderHeader;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderStatus;
import com.monsoon.seedflowplus.domain.sales.order.repository.OrderDetailRepository;
import com.monsoon.seedflowplus.domain.sales.order.repository.OrderHeaderRepository;
import com.monsoon.seedflowplus.domain.schedule.dto.command.DealScheduleUpsertCommand;
import com.monsoon.seedflowplus.domain.schedule.sync.DealScheduleSyncService;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderHeaderRepository orderHeaderRepository;

    @Mock
    private OrderDetailRepository orderDetailRepository;

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ContractDetailRepository contractDetailRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StatementService statementService;

    @Mock
    private DealPipelineFacade dealPipelineFacade;

    @Mock
    private DealLogQueryService dealLogQueryService;

    @Mock
    private DealScheduleSyncService dealScheduleSyncService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderHeaderRepository,
                orderDetailRepository,
                contractRepository,
                contractDetailRepository,
                clientRepository,
                employeeRepository,
                userRepository,
                statementService,
                dealPipelineFacade,
                dealLogQueryService,
                dealScheduleSyncService
        );
        doNothing().when(dealScheduleSyncService).upsertFromEvent(any());
    }

    @Test
    void createOrderShouldRecordCreateLog() {
        Long clientId = 7L;
        Long orderId = 31L;
        Long orderDetailId = 41L;
        Long contractId = 11L;
        Long contractDetailId = 21L;
        AtomicReference<OrderDetail> savedDetailRef = new AtomicReference<>();

        SalesDeal deal = org.mockito.Mockito.mock(SalesDeal.class);
        when(deal.getCurrentStage()).thenReturn(DealStage.APPROVED);

        Employee employee = org.mockito.Mockito.mock(Employee.class);
        when(employee.getId()).thenReturn(3L);

        Client client = org.mockito.Mockito.mock(Client.class);
        when(client.getId()).thenReturn(clientId);

        ContractHeader contract = org.mockito.Mockito.mock(ContractHeader.class);
        when(contract.getStartDate()).thenReturn(LocalDate.now().minusDays(1));
        when(contract.getEndDate()).thenReturn(LocalDate.now().plusDays(1));
        when(contract.getDeal()).thenReturn(deal);
        when(contract.getAuthor()).thenReturn(employee);
        when(contract.getId()).thenReturn(contractId);

        ContractDetail contractDetail = org.mockito.Mockito.mock(ContractDetail.class);
        when(contractDetail.getId()).thenReturn(contractDetailId);
        when(contractDetail.getTotalQuantity()).thenReturn(10);
        when(contractDetail.getUnitPrice()).thenReturn(new BigDecimal("1500"));

        OrderCreateRequest request = new OrderCreateRequest();
        ReflectionTestUtils.setField(request, "headerId", contractId);
        ReflectionTestUtils.setField(request, "shippingName", "테스트 물류센터");
        ReflectionTestUtils.setField(request, "shippingPhone", "010-1111-2222");
        ReflectionTestUtils.setField(request, "shippingAddress", "서울시 강남구");
        ReflectionTestUtils.setField(request, "shippingAddressDetail", "101호");
        ReflectionTestUtils.setField(request, "deliveryRequest", "문 앞 배송");

        OrderDetailRequest itemRequest = new OrderDetailRequest();
        ReflectionTestUtils.setField(itemRequest, "contractDetailId", contractDetailId);
        ReflectionTestUtils.setField(itemRequest, "quantity", 2L);
        ReflectionTestUtils.setField(request, "items", List.of(itemRequest));

        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(contractDetailRepository.findById(contractDetailId)).thenReturn(Optional.of(contractDetail));
        when(orderDetailRepository.sumQuantityByContractDetailId(contractDetailId)).thenReturn(0L);
        when(orderHeaderRepository.existsByOrderCode(any())).thenReturn(false);
        when(orderHeaderRepository.save(any(OrderHeader.class))).thenAnswer(invocation -> {
            OrderHeader saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", orderId);
            return saved;
        });
        when(orderDetailRepository.save(any(OrderDetail.class))).thenAnswer(invocation -> {
            OrderDetail saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", orderDetailId);
            savedDetailRef.set(saved);
            return saved;
        });
        when(orderDetailRepository.findByOrderHeader_Id(orderId)).thenAnswer(invocation -> List.of(savedDetailRef.get()));
        when(dealLogQueryService.getRecentDocumentLogs(any(), any(), any())).thenReturn(List.of());

        OrderResponse response = orderService.createOrder(request, clientId);

        ArgumentCaptor<List<DealLogWriteService.DiffField>> diffCaptor = ArgumentCaptor.forClass(List.class);
        verify(dealPipelineFacade).recordAndSync(
                eq(deal),
                eq(DealType.ORD),
                eq(orderId),
                eq(response.getOrderCode()),
                eq(DealStage.APPROVED),
                eq(DealStage.IN_PROGRESS),
                eq(OrderStatus.PENDING.name()),
                eq(OrderStatus.PENDING.name()),
                eq(ActionType.CREATE),
                isNull(),
                eq(ActorType.CLIENT),
                eq(clientId),
                isNull(),
                diffCaptor.capture()
        );
        assertEquals(orderId, response.getOrderId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(2, diffCaptor.getValue().size());
    }

    @Test
    void confirmOrderShouldUpsertDeliveryDueSchedule() {

        Long orderId = 31L;
        Long clientId = 7L;
        Long ownerEmployeeId = 12L;
        Long assigneeUserId = 99L;

        Client client = Client.builder()
                .clientCode("C-1")
                .clientName("테스트 거래처")
                .build();
        ReflectionTestUtils.setField(client, "id", clientId);

        Employee ownerEmployee = Employee.builder()
                .employeeName("담당 영업")
                .build();
        ReflectionTestUtils.setField(ownerEmployee, "id", ownerEmployeeId);

        SalesDeal deal = org.mockito.Mockito.mock(SalesDeal.class);
        when(deal.getId()).thenReturn(55L);
        when(deal.getOwnerEmp()).thenReturn(ownerEmployee);

        Employee orderEmployee = org.mockito.Mockito.mock(Employee.class);
        when(orderEmployee.getId()).thenReturn(3L);

        OrderHeader orderHeader = createOrderHeader(orderId, client, deal, orderEmployee);

        User assigneeUser = User.builder()
                .loginId("sales")
                .loginPw("pw")
                .employee(ownerEmployee)
                .build();
        ReflectionTestUtils.setField(assigneeUser, "id", assigneeUserId);

        CustomUserDetails principal = org.mockito.Mockito.mock(CustomUserDetails.class);
        when(principal.getRole()).thenReturn(Role.CLIENT);
        when(principal.getClientId()).thenReturn(clientId);

        when(orderHeaderRepository.findById(orderId))
                .thenReturn(Optional.of(orderHeader));

        when(userRepository.findByEmployeeId(ownerEmployeeId))
                .thenReturn(Optional.of(assigneeUser));

        OrderDetail orderDetail = org.mockito.Mockito.mock(OrderDetail.class);
        ContractDetail contractDetail = org.mockito.Mockito.mock(ContractDetail.class);

        when(orderDetail.getId()).thenReturn(71L);
        when(orderDetail.getContractDetail()).thenReturn(contractDetail);
        when(contractDetail.getId()).thenReturn(21L);

        when(orderDetail.getQuantity()).thenReturn(1L);
        when(orderDetail.getShippingName()).thenReturn("테스트 물류센터");
        when(orderDetail.getShippingPhone()).thenReturn("010-1111-2222");
        when(orderDetail.getShippingAddress()).thenReturn("서울시 강남구");
        when(orderDetail.getShippingAddressDetail()).thenReturn("101호");
        when(orderDetail.getDeliveryRequest()).thenReturn("문 앞 배송");

        when(orderDetailRepository.findByOrderHeader_Id(orderId))
                .thenReturn(List.of(orderDetail));

        when(dealLogQueryService.getRecentDocumentLogs(any(), any(), any()))
                .thenReturn(List.of());

        // pipeline validation mock
        doNothing().when(dealPipelineFacade)
                .validateTransitionOrThrow(any(), any(), any(), any());

        OrderResponse response = orderService.confirmOrder(orderId, principal);

        ArgumentCaptor<DealScheduleUpsertCommand> captor =
                ArgumentCaptor.forClass(DealScheduleUpsertCommand.class);

        verify(dealScheduleSyncService).upsertFromEvent(captor.capture());

        DealScheduleUpsertCommand command = captor.getValue();

        assertEquals("ORD_31_DELIVERY_DUE", command.externalKey());
        assertEquals("납품 예정: 테스트 거래처", command.title());
        assertEquals(LocalDateTime.of(2026,3,10,0,0), command.startAt());
        assertEquals(LocalDateTime.of(2026,3,11,0,0), command.endAt());

        assertEquals(orderId, response.getOrderId());
        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
    }

    @Test
    void confirmOrderShouldCreateStatementAfterOrderLogAndScheduleSync() {
        Long orderId = 31L;
        Long clientId = 7L;
        Long ownerEmployeeId = 12L;
        Long assigneeUserId = 99L;

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
        ReflectionTestUtils.setField(client, "id", clientId);

        Employee ownerEmployee = Employee.builder()
                .employeeCode("EMP-1")
                .employeeName("담당 영업")
                .employeeEmail("owner@test.com")
                .employeePhone("010-1111-1111")
                .address("서울")
                .build();
        ReflectionTestUtils.setField(ownerEmployee, "id", ownerEmployeeId);

        SalesDeal deal = org.mockito.Mockito.mock(SalesDeal.class);
        when(deal.getId()).thenReturn(55L);
        when(deal.getOwnerEmp()).thenReturn(ownerEmployee);

        Employee orderEmployee = org.mockito.Mockito.mock(Employee.class);
        when(orderEmployee.getId()).thenReturn(3L);

        OrderHeader orderHeader = createOrderHeader(orderId, client, deal, orderEmployee);

        User assigneeUser = User.builder()
                .loginId("sales")
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.SALES_REP)
                .employee(ownerEmployee)
                .build();
        ReflectionTestUtils.setField(assigneeUser, "id", assigneeUserId);

        CustomUserDetails principal = org.mockito.Mockito.mock(CustomUserDetails.class);
        when(principal.getRole()).thenReturn(Role.CLIENT);
        when(principal.getClientId()).thenReturn(clientId);
        when(orderHeaderRepository.findById(orderId)).thenReturn(Optional.of(orderHeader));
        when(userRepository.findByEmployeeId(ownerEmployeeId)).thenReturn(Optional.of(assigneeUser));
        OrderDetail orderDetail = org.mockito.Mockito.mock(OrderDetail.class);
        when(orderDetail.getId()).thenReturn(71L);
        when(orderDetail.getContractDetail()).thenReturn(org.mockito.Mockito.mock(ContractDetail.class));
        when(orderDetail.getQuantity()).thenReturn(1L);
        when(orderDetail.getShippingName()).thenReturn("테스트 물류센터");
        when(orderDetail.getShippingPhone()).thenReturn("010-1111-2222");
        when(orderDetail.getShippingAddress()).thenReturn("서울시 강남구");
        when(orderDetail.getShippingAddressDetail()).thenReturn("101호");
        when(orderDetail.getDeliveryRequest()).thenReturn("문 앞 배송");
        ContractDetail detailContract = orderDetail.getContractDetail();
        when(detailContract.getId()).thenReturn(21L);
        when(orderDetailRepository.findByOrderHeader_Id(orderId)).thenReturn(List.of(orderDetail));
        when(dealLogQueryService.getRecentDocumentLogs(any(), any(), any())).thenReturn(List.of());

        orderService.confirmOrder(orderId, principal);

        InOrder inOrder = inOrder(
                dealPipelineFacade,
                dealScheduleSyncService,
                statementService
        );

        inOrder.verify(dealPipelineFacade).recordAndSync(any(), any(), any(), any(),
                any(), any(), any(), any(),
                eq(ActionType.CONFIRM),
                any(), any(), any(), any(), any());

        inOrder.verify(dealScheduleSyncService)
                .upsertFromEvent(any());

        inOrder.verify(statementService)
                .createStatement(any(), any(), any());
    }

    @Test
    void confirmOrderShouldFallbackToPrincipalUserWhenOwnerUserMissing() {
        Long orderId = 31L;
        Long clientId = 41L;
        Long ownerEmployeeId = 51L;

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
        ReflectionTestUtils.setField(client, "id", clientId);

        Employee ownerEmployee = Employee.builder()
                .employeeCode("EMP-1")
                .employeeName("담당 영업")
                .employeeEmail("owner@test.com")
                .employeePhone("010-1111-1111")
                .address("서울")
                .build();
        ReflectionTestUtils.setField(ownerEmployee, "id", ownerEmployeeId);

        SalesDeal deal = org.mockito.Mockito.mock(SalesDeal.class);
        when(deal.getId()).thenReturn(55L);
        when(deal.getOwnerEmp()).thenReturn(ownerEmployee);

        Employee orderEmployee = org.mockito.Mockito.mock(Employee.class);
        when(orderEmployee.getId()).thenReturn(3L);

        OrderHeader orderHeader = createOrderHeader(orderId, client, deal, orderEmployee);

        CustomUserDetails principal = org.mockito.Mockito.mock(CustomUserDetails.class);
        when(principal.getRole()).thenReturn(Role.CLIENT);
        when(principal.getClientId()).thenReturn(clientId);
        when(principal.getUserId()).thenReturn(7001L);

        when(orderHeaderRepository.findById(orderId)).thenReturn(Optional.of(orderHeader));
        when(userRepository.findByEmployeeId(ownerEmployeeId)).thenReturn(Optional.empty());
        OrderDetail orderDetail = org.mockito.Mockito.mock(OrderDetail.class);
        when(orderDetail.getId()).thenReturn(71L);
        when(orderDetail.getContractDetail()).thenReturn(org.mockito.Mockito.mock(ContractDetail.class));
        when(orderDetail.getQuantity()).thenReturn(1L);
        when(orderDetail.getShippingName()).thenReturn("테스트 물류센터");
        when(orderDetail.getShippingPhone()).thenReturn("010-1111-2222");
        when(orderDetail.getShippingAddress()).thenReturn("서울시 강남구");
        when(orderDetail.getShippingAddressDetail()).thenReturn("101호");
        when(orderDetail.getDeliveryRequest()).thenReturn("문 앞 배송");
        ContractDetail detailContract = orderDetail.getContractDetail();
        when(detailContract.getId()).thenReturn(21L);
        when(orderDetailRepository.findByOrderHeader_Id(orderId)).thenReturn(List.of(orderDetail));
        when(dealLogQueryService.getRecentDocumentLogs(any(), any(), any())).thenReturn(List.of());

        orderService.confirmOrder(orderId, principal);

        ArgumentCaptor<DealScheduleUpsertCommand> commandCaptor = ArgumentCaptor.forClass(DealScheduleUpsertCommand.class);
        verify(dealScheduleSyncService).upsertFromEvent(commandCaptor.capture());
        assertEquals(7001L, commandCaptor.getValue().assigneeUserId());
    }

    private OrderHeader createOrderHeader(
            Long orderId,
            Client client,
            SalesDeal deal,
            Employee orderEmployee
    ) {
        ContractHeader contract = org.mockito.Mockito.mock(ContractHeader.class);
        when(contract.getId()).thenReturn(11L);

        OrderHeader orderHeader =
                OrderHeader.create(contract, client, deal, orderEmployee, "ORD-20260310-001");

        ReflectionTestUtils.setField(orderHeader, "id", orderId);
        ReflectionTestUtils.setField(orderHeader, "createdAt",
                LocalDateTime.of(2026, 3, 10, 15, 30));

        ReflectionTestUtils.setField(orderHeader, "deliveryDate",
                LocalDate.of(2026, 3, 10));

        return orderHeader;
    }

}
