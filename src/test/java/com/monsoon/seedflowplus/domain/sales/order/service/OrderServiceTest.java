package com.monsoon.seedflowplus.domain.sales.order.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.EmployeeRepository;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private StatementService statementService;

    @Mock
    private DealPipelineFacade dealPipelineFacade;

    @Mock
    private DealLogQueryService dealLogQueryService;

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
                statementService,
                dealPipelineFacade,
                dealLogQueryService
        );
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
}
