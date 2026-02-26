package com.monsoon.seedflowplus.domain.billing.invoice.dto.response;

import com.monsoon.seedflowplus.domain.billing.invoice.entity.Invoice;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatement;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class InvoiceResponse {

    private Long invoiceId;
    private String invoiceCode;
    private Long contractId;
    private Long clientId;
    private Long employeeId;
    private LocalDate invoiceDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal supplyAmount;
    private BigDecimal vatAmount;
    private BigDecimal totalAmount;
    private InvoiceStatus status;
    private LocalDateTime createdAt;
    private List<InvoiceStatementItem> statements;

    @Getter
    @Builder
    public static class InvoiceStatementItem {
        private Long statementId;
        private String statementCode;
        private BigDecimal totalAmount;
        private boolean included;
    }

    public static InvoiceResponse of(Invoice invoice, List<InvoiceStatement> invoiceStatements) {
        List<InvoiceStatementItem> statementItems = invoiceStatements.stream()
                .map(is -> InvoiceStatementItem.builder()
                        .statementId(is.getStatement().getId())
                        .statementCode(is.getStatement().getStatementCode())
                        .totalAmount(is.getStatement().getTotalAmount())
                        .included(is.isIncluded())
                        .build())
                .toList();

        return InvoiceResponse.builder()
                .invoiceId(invoice.getId())
                .invoiceCode(invoice.getInvoiceCode())
                .contractId(invoice.getContractId())
                .clientId(invoice.getClient() != null ? invoice.getClient().getId() : null)
                .employeeId(invoice.getEmployee() != null ? invoice.getEmployee().getId() : null)
                .invoiceDate(invoice.getInvoiceDate())
                .startDate(invoice.getStartDate())
                .endDate(invoice.getEndDate())
                .supplyAmount(invoice.getSupplyAmount())
                .vatAmount(invoice.getVatAmount())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .createdAt(invoice.getCreatedAt())
                .statements(statementItems)
                .build();
    }
}