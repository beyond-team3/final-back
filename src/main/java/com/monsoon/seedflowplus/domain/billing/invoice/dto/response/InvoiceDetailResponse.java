package com.monsoon.seedflowplus.domain.billing.invoice.dto.response;

import com.monsoon.seedflowplus.domain.billing.invoice.entity.Invoice;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatement;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatus;
import com.monsoon.seedflowplus.domain.deal.log.dto.response.DealLogSummaryDto;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// 영업사원용 dto
@Getter
@Builder
public class InvoiceDetailResponse {

    private Long invoiceId;
    private String invoiceCode;
    private Long contractId;
    private String contractCode;   // 추가: 프론트 계약코드 표시용
    private Long clientId;
    private String clientName;     // 추가: 프론트 거래처명 표시용
    private Long employeeId;
    private LocalDate invoiceDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal supplyAmount;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;
    private InvoiceStatus status;
    private String memo;
    private LocalDateTime createdAt;
    private List<StatementItem> statements;
    private List<DealLogSummaryDto> recentLogs;

    // ── StatementItem ─────────────────────────────────────────────
    // InvoiceResponse.InvoiceStatementItem 대신 이 클래스 내부에 정의
    // supplyAmount, vatAmount 추가 → 프론트 금액 계산에 필요
    @Getter
    @Builder
    public static class StatementItem {
        private Long statementId;
        private String statementCode;
        private BigDecimal supplyAmount;   // 추가
        private BigDecimal vatAmount;      // 추가
        private BigDecimal totalAmount;
        private boolean included;
    }

    public static InvoiceDetailResponse of(Invoice invoice, List<InvoiceStatement> invoiceStatements) {
        return of(invoice, invoiceStatements, List.of(), null);
    }

    public static InvoiceDetailResponse of(
            Invoice invoice,
            List<InvoiceStatement> invoiceStatements,
            List<DealLogSummaryDto> recentLogs
    ) {
        return of(invoice, invoiceStatements, recentLogs, null);
    }

    // contractCode를 외부에서 주입하는 오버로드 (InvoiceService에서 사용)
    public static InvoiceDetailResponse of(
            Invoice invoice,
            List<InvoiceStatement> invoiceStatements,
            List<DealLogSummaryDto> recentLogs,
            String contractCode
    ) {
        List<StatementItem> statementItems = invoiceStatements.stream()
                .map(is -> {
                    BigDecimal total  = is.getStatement().getTotalAmount() != null
                            ? is.getStatement().getTotalAmount() : BigDecimal.ZERO;
                    BigDecimal supply = is.getStatement().getSupplyAmount() != null
                            ? is.getStatement().getSupplyAmount()
                            : total.compareTo(BigDecimal.ZERO) > 0
                            ? total.divide(BigDecimal.valueOf(1.1), 0, java.math.RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    BigDecimal vat = total.subtract(supply);

                    return StatementItem.builder()
                            .statementId(is.getStatement().getId())
                            .statementCode(is.getStatement().getStatementCode())
                            .supplyAmount(supply)
                            .vatAmount(vat)
                            .totalAmount(total)
                            .included(is.isIncluded())
                            .build();
                })
                .toList();

        String clientName = invoice.getClient() != null
                ? invoice.getClient().getClientName()
                : null;

        return InvoiceDetailResponse.builder()
                .invoiceId(invoice.getId())
                .invoiceCode(invoice.getInvoiceCode())
                .contractId(invoice.getContractId())
                .contractCode(contractCode)
                .clientId(invoice.getClient() != null ? invoice.getClient().getId() : null)
                .clientName(clientName)
                .employeeId(invoice.getEmployee() != null ? invoice.getEmployee().getId() : null)
                .invoiceDate(invoice.getInvoiceDate())
                .startDate(invoice.getStartDate())
                .endDate(invoice.getEndDate())
                .supplyAmount(invoice.getSupplyAmount())
                .vatAmount(invoice.getVatAmount())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .memo(invoice.getMemo())
                .createdAt(invoice.getCreatedAt())
                .statements(statementItems)
                .recentLogs(recentLogs)
                .build();
    }
}