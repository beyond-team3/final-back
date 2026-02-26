package com.monsoon.seedflowplus.domain.billing.invoice.dto.response;

import com.monsoon.seedflowplus.domain.billing.invoice.entity.Invoice;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.InvoiceStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InvoicePublishResponse {

    private Long invoiceId;
    private String invoiceCode;
    private InvoiceStatus status;

    public static InvoicePublishResponse from(Invoice invoice) {
        return InvoicePublishResponse.builder()
                .invoiceId(invoice.getId())
                .invoiceCode(invoice.getInvoiceCode())
                .status(invoice.getStatus())
                .build();
    }
}