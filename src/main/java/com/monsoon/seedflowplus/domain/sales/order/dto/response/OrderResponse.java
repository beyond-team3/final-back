package com.monsoon.seedflowplus.domain.sales.order.dto.response;

import com.monsoon.seedflowplus.domain.sales.order.entity.OrderStatus;
import com.monsoon.seedflowplus.domain.deal.log.dto.response.DealLogSummaryDto;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderResponse {

    private Long orderId;
    private String orderCode;
    private Long headerId; // reason: 응답에서도 계약 헤더 식별자 명칭을 headerId로 통일해 요청 필드와 일관성 유지
    private Long clientId;
    private Long employeeId;

    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;

    // 배송 정보
    private String shippingName;
    private String shippingPhone;
    private String shippingAddress;
    private String shippingAddressDetail;
    private String deliveryRequest;
    private LocalDate deliveryDate;

    private List<OrderDetailResponse> items;
    private List<DealLogSummaryDto> recentLogs;
}
