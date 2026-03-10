package com.monsoon.seedflowplus.domain.sales.order.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.repository.ClientRepository;
import com.monsoon.seedflowplus.domain.account.repository.EmployeeRepository;
import com.monsoon.seedflowplus.domain.billing.statement.service.StatementService;
import com.monsoon.seedflowplus.domain.deal.common.ActionType;
import com.monsoon.seedflowplus.domain.deal.common.ActorType;
import com.monsoon.seedflowplus.domain.deal.common.DealStage;
import com.monsoon.seedflowplus.domain.deal.common.DealType;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogWriteService;
import com.monsoon.seedflowplus.domain.deal.log.service.DealLogQueryService;
import com.monsoon.seedflowplus.domain.deal.log.service.DealPipelineFacade;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractDetail;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractDetailRepository;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import com.monsoon.seedflowplus.domain.sales.order.dto.request.OrderCreateRequest;
import com.monsoon.seedflowplus.domain.sales.order.dto.request.OrderDetailRequest;
import com.monsoon.seedflowplus.domain.sales.order.dto.response.*;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderDetail;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderHeader;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderStatus;
import com.monsoon.seedflowplus.domain.sales.order.repository.OrderDetailRepository;
import com.monsoon.seedflowplus.domain.sales.order.repository.OrderHeaderRepository;
import com.monsoon.seedflowplus.infra.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
    private final OrderHeaderRepository orderHeaderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ContractRepository contractHeaderRepository;
    private final ContractDetailRepository contractDetailRepository;
    private final ClientRepository clientRepository;
    private final EmployeeRepository employeeRepository;
    private final StatementService statementService;
    private final DealPipelineFacade dealPipelineFacade;
    private final DealLogQueryService dealLogQueryService;


    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, Long clientId) {

        // 1. 계약 조회
        ContractHeader contract = contractHeaderRepository.findById(request.getHeaderId()) // reason: 주문 입력에서 계약 헤더 식별자 필드를 headerId로 명시적으로 사용
                .orElseThrow(() -> new CoreException(ErrorType.CONTRACT_NOT_FOUND));

        // 2. 계약 기간 검증
        LocalDate today = LocalDate.now();
        if (today.isBefore(contract.getStartDate()) || today.isAfter(contract.getEndDate())) {
            throw new CoreException(ErrorType.CONTRACT_EXPIRED);
        }
        if (contract.getDeal() == null) {
            throw new CoreException(ErrorType.DEAL_NOT_FOUND);
        }

        // 3. 거래처 / 영업사원 조회
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));
        Employee employee = contract.getAuthor();

        // 4. 주문 코드 생성
        String orderCode = generateCode("ORD");

        // 5. OrderHeader 생성
        OrderHeader orderHeader = OrderHeader.create(contract, client, contract.getDeal(), employee, orderCode);
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

        // ORD도 다른 문서와 동일하게 생성 완료 시점의 CREATE 로그를 남겨야 타임라인이 누락되지 않는다.
        dealPipelineFacade.recordAndSync(
                orderHeader.getDeal(),
                DealType.ORD,
                orderHeader.getId(),
                orderHeader.getOrderCode(),
                orderHeader.getDeal().getCurrentStage(),
                mapOrderStage(orderHeader.getStatus()),
                orderHeader.getStatus().name(),
                orderHeader.getStatus().name(),
                ActionType.CREATE,
                null,
                ActorType.CLIENT,
                clientId,
                null,
                List.of(
                        new DealLogWriteService.DiffField("totalAmount", "주문 총액", null, totalAmount, "MONEY"),
                        new DealLogWriteService.DiffField("itemCount", "주문 품목 수", null, request.getItems().size(), "COUNT"))
        );

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

        String fromStatus = orderHeader.getStatus().name();
        DealStage fromStage = mapOrderStage(orderHeader.getStatus());
        dealPipelineFacade.validateTransitionOrThrow(
                DealType.ORD,
                fromStatus,
                ActionType.CANCEL,
                OrderStatus.CANCELED.name()
        );

        orderHeader.cancel();

        dealPipelineFacade.recordAndSync(
                orderHeader.getDeal(),
                DealType.ORD,
                orderHeader.getId(),
                orderHeader.getOrderCode(),
                fromStage,
                DealStage.CANCELED,
                fromStatus,
                OrderStatus.CANCELED.name(),
                ActionType.CANCEL,
                null,
                ActorType.CLIENT,
                clientId,
                null,
                List.of(new DealLogWriteService.DiffField("status", "주문 상태", fromStatus, OrderStatus.CANCELED.name(), "STATUS"))
        );

        return OrderCancelResponse.builder()
                .orderId(orderHeader.getId())
                .status(orderHeader.getStatus())
                .build();
    }

    // 주문 확정
    @Transactional
    public OrderResponse confirmOrder(Long orderId, CustomUserDetails principal) {
        OrderHeader orderHeader = orderHeaderRepository.findById(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.ORDER_NOT_FOUND));

        if (orderHeader.getStatus() != OrderStatus.PENDING) {
            throw new CoreException(ErrorType.ORDER_ALREADY_CONFIRMED);
        }

        String fromStatus = orderHeader.getStatus().name();
        DealStage fromStage = mapOrderStage(orderHeader.getStatus());
        dealPipelineFacade.validateTransitionOrThrow(
                DealType.ORD,
                fromStatus,
                ActionType.CONFIRM,
                OrderStatus.CONFIRMED.name()
        );

        orderHeader.confirm();

        ActorType actorType = resolveActorType(principal);
        Long actorId = resolveActorId(actorType, principal);
        dealPipelineFacade.recordAndSync(
                orderHeader.getDeal(),
                DealType.ORD,
                orderHeader.getId(),
                orderHeader.getOrderCode(),
                fromStage,
                DealStage.CONFIRMED,
                fromStatus,
                OrderStatus.CONFIRMED.name(),
                ActionType.CONFIRM,
                null,
                actorType,
                actorId,
                null,
                List.of(new DealLogWriteService.DiffField("status", "주문 상태", fromStatus, OrderStatus.CONFIRMED.name(), "STATUS"))
        );

        statementService.createStatement(orderHeader, actorType, actorId);

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
                .headerId(orderHeader.getContract().getId()) // reason: 응답 필드명 변경(contractId->headerId)에 맞춰 매핑 동기화
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
                .recentLogs(dealLogQueryService.getRecentDocumentLogs(
                        orderHeader.getDeal() != null ? orderHeader.getDeal().getId() : null,
                        DealType.ORD,
                        orderHeader.getId()
                ))
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

    private DealStage mapOrderStage(OrderStatus status) {
        return switch (status) {
            case PENDING -> DealStage.IN_PROGRESS;
            case CONFIRMED -> DealStage.CONFIRMED;
            case CANCELED -> DealStage.CANCELED;
        };
    }

    private ActorType resolveActorType(CustomUserDetails principal) {
        if (principal == null || principal.getRole() == null) {
            return ActorType.SYSTEM;
        }
        return switch (principal.getRole()) {
            case CLIENT -> ActorType.CLIENT;
            case ADMIN -> ActorType.ADMIN;
            default -> ActorType.SALES_REP;
        };
    }

    private Long resolveActorId(ActorType actorType, CustomUserDetails principal) {
        if (actorType == ActorType.SYSTEM) {
            return null;
        }
        if (principal == null) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
        Long actorId = actorType == ActorType.CLIENT
                ? principal.getClientId()
                : principal.getEmployeeId();
        if (actorId == null) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
        return actorId;
    }

    // ----------------------------------------------------------------
    // 거래처 거래 요약 조회 (영업사원/관리자 - 거래처 상세 페이지)
    // ----------------------------------------------------------------

    /**
     * 특정 거래처의 이번달 거래 요약 + 여신 정보를 조회합니다.
     * - 영업사원/관리자만 호출 가능 (Controller 레이어에서 권한 검증)
     *
     * @param clientId 조회 대상 거래처 ID
     * @return 이번달 주문 집계 + 여신 한도/미수금/잔여여신
     */
    public OrderTradeSummaryResponse getTradeSummary(Long clientId) {
        // 1. 거래처 조회 (여신 정보 포함)
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new CoreException(ErrorType.CLIENT_NOT_FOUND));

        // 2. 이번달 시작 / 다음달 시작 계산
        LocalDate today = LocalDate.now();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);

        // 3. 이번달 주문 집계
        BigDecimal thisMonthTotal = orderHeaderRepository.sumThisMonthTotalAmount(
                clientId, startOfMonth, startOfNextMonth
        );
        long inProgressCount = orderHeaderRepository.countThisMonthByStatus(
                clientId, OrderStatus.PENDING, startOfMonth, startOfNextMonth
        );
        long completedCount = orderHeaderRepository.countThisMonthByStatus(
                clientId, OrderStatus.CONFIRMED, startOfMonth, startOfNextMonth
        );

        // 4. 여신 정보 (null-safe)
        BigDecimal totalCredit = client.getTotalCredit() != null ? client.getTotalCredit() : BigDecimal.ZERO;
        BigDecimal usedCredit = client.getUsedCredit() != null ? client.getUsedCredit() : BigDecimal.ZERO;
        BigDecimal remainingCredit = totalCredit.subtract(usedCredit);

        return OrderTradeSummaryResponse.builder()
                .thisMonth(OrderTradeSummaryResponse.ThisMonth.builder()
                        .totalAmount(thisMonthTotal)
                        .inProgressCount(inProgressCount)
                        .completedCount(completedCount)
                        .build())
                .totalCredit(totalCredit)
                .usedCredit(usedCredit)
                .remainingCredit(remainingCredit)
                .build();
    }

}
