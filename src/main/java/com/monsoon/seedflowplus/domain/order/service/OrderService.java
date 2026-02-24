package com.monsoon.seedflowplus.domain.order.service;

import com.monsoon.seedflowplus.domain.account.Client;
import com.monsoon.seedflowplus.domain.account.Employee;
import com.monsoon.seedflowplus.domain.order.dto.request.OrderCreateRequest;
import com.monsoon.seedflowplus.domain.order.dto.response.OrderResponse;
import com.monsoon.seedflowplus.domain.order.repository.OrderDetailRepository;
import com.monsoon.seedflowplus.domain.order.repository.OrderHeaderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    private final OrderHeaderRepository orderHeaderRepository;
    private final OrderDetailRepository orderDetailRepository;
    //private final ContractService contractService; // 한도 조회용 (타 파트)
    //private final StatementService statementService; // 주문 완료 시 호출

    //계약 잔여 수량 검증 로직
    /*private void validateRemainingQuantity(Long contractDetailPk, Long requestQty) {

        Long contractLimit = contractService.getContractDetailLimit(contractDetailPk);
        // ex: 계약 수량 100

        Long orderedQty = orderDetailRepository
                .sumQuantityByContractDetailPk(contractDetailPk);
        // ex: 이미 주문된 수량 60

        if (orderedQty + requestQty > contractLimit) {
            throw new IllegalArgumentException("계약 잔여 수량을 초과했습니다.");
        }
    }*/

    // 주문 생성 로직
    /*@Transactional
    public OrderResponse createOrder(OrderCreateRequest request, Client client, Employee employee) {

        String orderCode = generateOrderCode();

        OrderHeader order = OrderHeader.create(
                request.getContractId(),
                client,
                employee,
                orderCode
        );

        orderHeaderRepository.save(order);

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderDetailRequest item : request.getItems()) {

            // 1️. 잔여 수량 검증
            validateRemainingQuantity(item.getContractDetailPk(), item.getQuantity());

            // 2. 디테일 생성
            OrderDetail detail = OrderDetail.create(
                    order,
                    item.getContractDetailPk(),
                    item.getQuantity()
            );

            orderDetailRepository.save(detail);

            // 3️. 금액 계산 (계약 단가 조회)
            BigDecimal unitPrice = contractService
                    .getUnitPrice(item.getContractDetailPk());

            totalAmount = totalAmount.add(
                    unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()))
            );
        }

        order.updateTotalAmount(totalAmount);

        return toResponse(order);
    }*/

}
