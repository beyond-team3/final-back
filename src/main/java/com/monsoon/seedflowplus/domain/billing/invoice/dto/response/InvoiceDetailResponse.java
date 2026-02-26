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

//영업사원용 dto
@Getter
@Builder
public class InvoiceDetailResponse {

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
    private String memo;            // 영업사원 전용
    private LocalDateTime createdAt;
    private List<InvoiceResponse.InvoiceStatementItem> statements;

    public static InvoiceDetailResponse of(Invoice invoice, List<InvoiceStatement> invoiceStatements) {
        List<InvoiceResponse.InvoiceStatementItem> statementItems = invoiceStatements.stream()
                .map(is -> InvoiceResponse.InvoiceStatementItem.builder()
                        .statementId(is.getStatement().getId())
                        .statementCode(is.getStatement().getStatementCode())
                        .totalAmount(is.getStatement().getTotalAmount())
                        .included(is.isIncluded())
                        .build())
                .toList();

        return InvoiceDetailResponse.builder()
                .invoiceId(invoice.getId())
                .invoiceCode(invoice.getInvoiceCode())
                .contractId(invoice.getContractId())
                .clientId(invoice.getClient().getId())
                .employeeId(invoice.getEmployee().getId())
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
                .build();
    }
}