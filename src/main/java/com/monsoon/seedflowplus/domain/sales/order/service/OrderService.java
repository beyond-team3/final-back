/*
package com.monsoon.seedflowplus.domain.sales.order.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.EmployeeRepository;
import com.monsoon.seedflowplus.domain.billing.statement.service.StatementService;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractDetail;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.order.dto.request.OrderCreateRequest;
import com.monsoon.seedflowplus.domain.sales.order.dto.request.OrderDetailRequest;
import com.monsoon.seedflowplus.domain.sales.order.dto.response.OrderCancelResponse;
import com.monsoon.seedflowplus.domain.sales.order.dto.response.OrderDetailResponse;
import com.monsoon.seedflowplus.domain.sales.order.dto.response.OrderListResponse;
import com.monsoon.seedflowplus.domain.sales.order.dto.response.OrderResponse;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderDetail;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderHeader;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderStatus;
import com.monsoon.seedflowplus.domain.sales.order.repository.OrderDetailRepository;
import com.monsoon.seedflowplus.domain.sales.order.repository.OrderHeaderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    private final OrderHeaderRepository orderHeaderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ContractHeaderRepository contractHeaderRepository;
    private final ContractDetailRepository contractDetailRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final StatementService statementService;


    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, Long clientId) {

        // 1. 계약 조회
        ContractHeader contract = contractHeaderRepository.findById(request.getContractId())
                .orElseThrow(() -> new CoreException(ErrorType.CONTRACT_NOT_FOUND));

        // 2. 계약 기간 검증
        LocalDate today = LocalDate.now();
        if (today.isBefore(contract.getStartDate()) || today.isAfter(contract.getEndDate())) {
            throw new CoreException(ErrorType.CONTRACT_EXPIRED);
        }

        // 3. 거래처 / 영업사원 조회
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));
        Employee employee = contract.getAuthor();

        // 4. 주문 코드 생성
        String orderCode = generateCode("ORD");

        // 5. OrderHeader 생성
        OrderHeader orderHeader = OrderHeader.create(contract, client, employee, orderCode);
        orderHeaderRepository.save(orderHeader);

        // 6. OrderDetail 생성 + 수량 검증 + total_amount 계산
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderDetailRequest item : request.getItems()) {

            // 계약 디테일 조회
            ContractDetail contractDetail = contractDetailRepository.findById(item.getContractDetailId())
                    .orElseThrow(() -> new CoreException(ErrorType.CONTRACT_DETAIL_NOT_FOUND));

            // 잔여 수량 검증
            Long orderedQuantity = orderDetailRepository.sumQuantityByContractDetailId(item.getContractDetailId());
            if (orderedQuantity == null) orderedQuantity = 0L;
            long remainQuantity = contractDetail.getTotalQuantity() - orderedQuantity;

            if (item.getQuantity() > remainQuantity) {
                throw new CoreException(ErrorType.ORDER_QUANTITY_EXCEEDED);
            }

            // OrderDetail 생성
            OrderDetail orderDetail = OrderDetail.create(
                    orderHeader,
                    contractDetail,
                    item.getQuantity(),
                    request.getShippingName(),
                    request.getShippingPhone(),
                    request.getShippingAddress(),
                    request.getShippingAddressDetail(),
                    request.getDeliveryRequest()
            );
            orderDetailRepository.save(orderDetail);

            // 금액 합산 (단가 × 수량)
            BigDecimal amount = contractDetail.getUnitPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(amount);
        }

        // 7. 총액 업데이트
        orderHeader.updateTotalAmount(totalAmount);

        return toOrderResponse(orderHeader);
    }

    // 주문 목록 조회
    public List<OrderListResponse> getOrders(Long clientId) {
        return orderHeaderRepository.findByClient_Id(clientId).stream()
                .map(this::toOrderListResponse)
                .toList();
    }

    // 주문 단건 조회
    public OrderResponse getOrder(Long orderId) {
        OrderHeader orderHeader = orderHeaderRepository.findById(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.ORDER_NOT_FOUND));
        return toOrderResponse(orderHeader);
    }

    // 주문 취소
    @Transactional
    public OrderCancelResponse cancelOrder(Long orderId, Long clientId) {
        OrderHeader orderHeader = orderHeaderRepository.findById(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.ORDER_NOT_FOUND));

        if (!orderHeader.getClient().getId().equals(clientId)) {
            throw new CoreException(ErrorType.ACCESS_DENIED);
        }

        orderHeader.cancel();

        return OrderCancelResponse.builder()
                .orderId(orderHeader.getId())
                .status(orderHeader.getStatus())
                .build();
    }

    // 주문 확정
    @Transactional
    public OrderResponse confirmOrder(Long orderId) {
        OrderHeader orderHeader = orderHeaderRepository.findById(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.ORDER_NOT_FOUND));

        if (orderHeader.getStatus() != OrderStatus.PENDING) {
            throw new CoreException(ErrorType.ORDER_ALREADY_CONFIRMED);
        }

        orderHeader.confirm();
        statementService.createStatement(orderHeader);

        return toOrderResponse(orderHeader);
    }

    // DTO 변환
    private OrderResponse toOrderResponse(OrderHeader orderHeader) {
        List<OrderDetail> details = orderDetailRepository.findByOrderHeader_Id(orderHeader.getId()); // 한 번만

        List<OrderDetailResponse> items = details.stream()
                .map(detail -> OrderDetailResponse.builder()
                        .orderDetailId(detail.getId())
                        .contractDetailId(detail.getContractDetail().getId())
                        .quantity(detail.getQuantity())
                        .build())
                .toList();

        OrderDetail firstDetail = details.stream().findFirst()
                .orElseThrow(() -> new CoreException(ErrorType.ORDER_DETAIL_NOT_FOUND));

        return OrderResponse.builder()
                .orderId(orderHeader.getId())
                .orderCode(orderHeader.getOrderCode())
                .contractId(orderHeader.getContract().getId())
                .clientId(orderHeader.getClient().getId())
                .employeeId(orderHeader.getEmployee().getId())
                .totalAmount(orderHeader.getTotalAmount())
                .status(orderHeader.getStatus())
                .createdAt(orderHeader.getCreatedAt())
                .shippingName(firstDetail.getShippingName())
                .shippingPhone(firstDetail.getShippingPhone())
                .shippingAddress(firstDetail.getShippingAddress())
                .shippingAddressDetail(firstDetail.getShippingAddressDetail())
                .deliveryRequest(firstDetail.getDeliveryRequest())
                .items(items)
                .build();
    }

    private OrderListResponse toOrderListResponse(OrderHeader orderHeader) {
        return OrderListResponse.builder()
                .orderId(orderHeader.getId())
                .orderCode(orderHeader.getOrderCode())
                .totalAmount(orderHeader.getTotalAmount())
                .status(orderHeader.getStatus())
                .createdAt(orderHeader.getCreatedAt())
                .build();
    }

    // 문서 코드 생성 (ORD-20260223-001)
    private String generateCode(String prefix) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String todayPrefix = prefix + "-" + date + "-";

        // 오늘 마지막 코드 조회해서 다음 번호 생성
        boolean exists = orderHeaderRepository.existsByOrderCode(todayPrefix + "001");
        if (!exists) return todayPrefix + "001";

        // 오늘 생성된 주문 수 기반으로 번호 증가
        long count = orderHeaderRepository.countByOrderCodeStartingWith(todayPrefix);
        return todayPrefix + String.format("%03d", count + 1);
    }

}
*/
