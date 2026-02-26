package com.monsoon.seedflowplus.domain.billing.invoice.dto.response;

import com.monsoon.seedflowplus.domain.billing.invoice.entity.Invoice;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class InvoiceListResponse {

    private Long invoiceId;
    private String invoiceCode;
    private Long contractId;
    private Long clientId;
    private LocalDate invoiceDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalAmount;
    private InvoiceStatus status;
    private LocalDateTime createdAt;

    public static InvoiceListResponse from(Invoice invoice) {
        return InvoiceListResponse.builder()
                .invoiceId(invoice.getId())
                .invoiceCode(invoice.getInvoiceCode())
                .contractId(invoice.getContractId())
                .clientId(invoice.getClient() != null ? invoice.getClient().getId() : null)
                .invoiceDate(invoice.getInvoiceDate())
                .startDate(invoice.getStartDate())
                .endDate(invoice.getEndDate())
                .totalAmount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}