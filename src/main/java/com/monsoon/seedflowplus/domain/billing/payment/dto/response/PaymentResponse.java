package com.monsoon.seedflowplus.domain.billing.payment.dto.response;

import com.monsoon.seedflowplus.domain.billing.payment.entity.Payment;
import com.monsoon.seedflowplus.domain.billing.payment.entity.PaymentMethod;
import com.monsoon.seedflowplus.domain.billing.payment.entity.PaymentStatus;
import com.monsoon.seedflowplus.domain.deal.log.dto.response.DealLogSummaryDto;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PaymentResponse {

    private Long paymentId;
    private String paymentCode;
    private Long invoiceId;
    private String invoiceCode;
    private Long clientId;
    private String clientName;
    private BigDecimal paymentAmount;
    private BigDecimal invoiceTotalAmount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private List<DealLogSummaryDto> recentLogs;

    public static PaymentResponse from(Payment payment) {
        return from(payment, List.of());
    }

    public static PaymentResponse from(Payment payment, List<DealLogSummaryDto> recentLogs) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .paymentCode(payment.getPaymentCode())
                .invoiceId(payment.getInvoice() != null ? payment.getInvoice().getId() : null)
                .invoiceCode(payment.getInvoice() != null ? payment.getInvoice().getInvoiceCode() : null)
                .clientId(payment.getClient() != null ? payment.getClient().getId() : null)
                .clientName(payment.getClient() != null ? payment.getClient().getClientName() : null)
                .paymentAmount(payment.getPaymentAmount())
                .invoiceTotalAmount(payment.getInvoice() != null ? payment.getInvoice().getTotalAmount() : null)
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getStatus() == PaymentStatus.COMPLETED ? payment.getCreatedAt() : null)
                .recentLogs(recentLogs)
                .build();
    }
}