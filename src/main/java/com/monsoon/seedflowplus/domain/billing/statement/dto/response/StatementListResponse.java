package com.monsoon.seedflowplus.domain.billing.statement.dto.response;

import com.monsoon.seedflowplus.domain.billing.statement.entity.Statement;
import com.monsoon.seedflowplus.domain.billing.statement.entity.StatementStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class StatementListResponse {

    private Long statementId;
    private String statementCode;
    private Long orderId;
    private String orderCode;
    private BigDecimal totalAmount;
    private StatementStatus status;
    private LocalDateTime createdAt;

    public static StatementListResponse from(Statement statement) {
        return StatementListResponse.builder()
                .statementId(statement.getId())
                .statementCode(statement.getStatementCode())
                .orderId(statement.getOrderHeader().getId())
                .orderCode(statement.getOrderHeader().getOrderCode())
                .totalAmount(statement.getTotalAmount())
                .status(statement.getStatus())
                .createdAt(statement.getCreatedAt())
                .build();
    }
}