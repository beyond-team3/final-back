package com.monsoon.seedflowplus.domain.billing.statement.dto.response;

import com.monsoon.seedflowplus.domain.billing.statement.entity.Statement;
import com.monsoon.seedflowplus.domain.billing.statement.entity.StatementStatus;
import com.monsoon.seedflowplus.domain.deal.log.dto.response.DealLogSummaryDto;
import com.monsoon.seedflowplus.domain.sales.contract.entity.BillingCycle;
import com.monsoon.seedflowplus.domain.sales.order.entity.OrderDetail;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Getter
@Builder
public class StatementResponse {

    private Long statementId;
    private String statementCode;
    private Long orderId;
    private String orderCode;
    private Long contractId;
    private String contractCode;
    private Long dealId;
    private Long clientId;
    private String clientName;
    private Long employeeId;
    private String employeeName;
    private BillingCycle billingCycle;
    private Long invoiceId;
    private BigDecimal supplyAmount;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;
    private StatementStatus status;
    private LocalDateTime createdAt;
    private java.time.LocalDate deliveryDate;
    private String shippingName;
    private String shippingPhone;
    private String shippingAddress;
    private String shippingAddressDetail;
    private String deliveryRequest;
    private List<StatementItem> items;
    private List<DealLogSummaryDto> recentLogs;

    @Getter
    @Builder
    public static class StatementItem {
        private Long orderDetailId;
        private Long contractDetailId;
        private String productName;
        private String productCategory;
        private Long quantity;
        private String unit;
    }

    public static StatementResponse from(Statement statement) {
        return from(statement, null, List.of(), List.of());
    }

    public static StatementResponse from(Statement statement, List<DealLogSummaryDto> recentLogs) {
        return from(statement, null, recentLogs, List.of());
    }

    public static StatementResponse from(Statement statement, Long invoiceId, List<DealLogSummaryDto> recentLogs) {
        return from(statement, invoiceId, recentLogs, List.of());
    }

    public static StatementResponse from(
            Statement statement,
            Long invoiceId,
            List<DealLogSummaryDto> recentLogs,
            List<OrderDetail> orderDetails
    ) {
        List<DealLogSummaryDto> safeRecentLogs = recentLogs != null ? recentLogs : Collections.emptyList();
        List<OrderDetail> safeOrderDetails = orderDetails != null ? orderDetails : Collections.emptyList();
        List<StatementItem> statementItems = safeOrderDetails.stream()
                .map(detail -> StatementItem.builder()
                        .orderDetailId(detail.getId())
                        .contractDetailId(detail.getContractDetail() != null ? detail.getContractDetail().getId() : null)
                        .productName(detail.getContractDetail() != null ? detail.getContractDetail().getProductName() : null)
                        .productCategory(detail.getContractDetail() != null ? detail.getContractDetail().getProductCategory() : null)
                        .quantity(detail.getQuantity())
                        .unit(detail.getContractDetail() != null ? detail.getContractDetail().getUnit() : null)
                        .build())
                .toList();
        OrderDetail firstDetail = safeOrderDetails.stream().findFirst().orElse(null);
        return StatementResponse.builder()
                .statementId(statement.getId())
                .statementCode(statement.getStatementCode())
                .orderId(statement.getOrderHeader().getId())
                .orderCode(statement.getOrderHeader().getOrderCode())
                .contractId(statement.getOrderHeader().getContract() != null ? statement.getOrderHeader().getContract().getId() : null)
                .contractCode(statement.getOrderHeader().getContract() != null ? statement.getOrderHeader().getContract().getContractCode() : null)
                .dealId(statement.getDeal() != null ? statement.getDeal().getId() : null)
                .clientId(statement.getOrderHeader().getClient() != null ? statement.getOrderHeader().getClient().getId() : null)
                .clientName(statement.getOrderHeader().getClient() != null ? statement.getOrderHeader().getClient().getClientName() : null)
                .employeeId(statement.getOrderHeader().getEmployee() != null ? statement.getOrderHeader().getEmployee().getId() : null)
                .employeeName(statement.getOrderHeader().getEmployee() != null ? statement.getOrderHeader().getEmployee().getEmployeeName() : null)
                .billingCycle(statement.getOrderHeader().getContract() != null ? statement.getOrderHeader().getContract().getBillingCycle() : null)
                .invoiceId(invoiceId)
                .supplyAmount(statement.getSupplyAmount())
                .vatAmount(statement.getVatAmount())
                .totalAmount(statement.getTotalAmount())
                .status(statement.getStatus())
                .createdAt(statement.getCreatedAt())
                .deliveryDate(statement.getOrderHeader().getDeliveryDate())
                .shippingName(firstDetail != null ? firstDetail.getShippingName() : null)
                .shippingPhone(firstDetail != null ? firstDetail.getShippingPhone() : null)
                .shippingAddress(firstDetail != null ? firstDetail.getShippingAddress() : null)
                .shippingAddressDetail(firstDetail != null ? firstDetail.getShippingAddressDetail() : null)
                .deliveryRequest(firstDetail != null ? firstDetail.getDeliveryRequest() : null)
                .items(statementItems)
                .recentLogs(safeRecentLogs)
                .build();
    }
}
