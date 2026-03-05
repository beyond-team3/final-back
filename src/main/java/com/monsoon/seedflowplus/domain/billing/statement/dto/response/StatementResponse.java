package com.monsoon.seedflowplus.domain.billing.statement.dto.response;

import com.monsoon.seedflowplus.domain.billing.statement.entity.Statement;
import com.monsoon.seedflowplus.domain.billing.statement.entity.StatementStatus;
import com.monsoon.seedflowplus.domain.deal.log.dto.response.DealLogSummaryDto;
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
    private Long invoiceId;
    private BigDecimal supplyAmount;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;
    private StatementStatus status;
    private LocalDateTime createdAt;
    private List<DealLogSummaryDto> recentLogs;

    public static StatementResponse from(Statement statement) {
        return from(statement, null, List.of());
    }

    public static StatementResponse from(Statement statement, List<DealLogSummaryDto> recentLogs) {
        return from(statement, null, recentLogs);
    }

    public static StatementResponse from(Statement statement, Long invoiceId, List<DealLogSummaryDto> recentLogs) {
        List<DealLogSummaryDto> safeRecentLogs = recentLogs != null ? recentLogs : Collections.emptyList();
        return StatementResponse.builder()
                .statementId(statement.getId())
                .statementCode(statement.getStatementCode())
                .orderId(statement.getOrderHeader().getId())
                .orderCode(statement.getOrderHeader().getOrderCode())
                .invoiceId(invoiceId)
                .supplyAmount(statement.getSupplyAmount())
                .vatAmount(statement.getVatAmount())
                .totalAmount(statement.getTotalAmount())
                .status(statement.getStatus())
                .createdAt(statement.getCreatedAt())
                .recentLogs(safeRecentLogs)
                .build();
    }
}
