package com.monsoon.seedflowplus.domain.billing.statement.dto.response;

import com.monsoon.seedflowplus.domain.billing.statement.entity.Statement;
import com.monsoon.seedflowplus.domain.billing.statement.entity.StatementStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class StatementResponse {

    private Long statementId;
    private String statementCode;
    private Long orderId;
    private String orderCode;
    private Long invoiceId;
    private BigDecimal supplyAmount;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;
    private StatementStatus status;
    private LocalDateTime createdAt;

    public static StatementResponse from(Statement statement) {
        return StatementResponse.builder()
                .statementId(statement.getId())
                .statementCode(statement.getStatementCode())
                .orderId(statement.getOrderHeader().getId())
                .orderCode(statement.getOrderHeader().getOrderCode())
                .invoiceId(statement.getInvoiceId())
                .supplyAmount(statement.getSupplyAmount())
                .vatAmount(statement.getVatAmount())
                .totalAmount(statement.getTotalAmount())
                .status(statement.getStatus())
                .createdAt(statement.getCreatedAt())
                .build();
    }
}