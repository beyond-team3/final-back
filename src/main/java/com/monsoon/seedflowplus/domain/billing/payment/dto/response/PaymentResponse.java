package com.monsoon.seedflowplus.domain.billing.payment.dto.response;

import com.monsoon.seedflowplus.domain.billing.payment.entity.Payment;
import com.monsoon.seedflowplus.domain.billing.payment.entity.PaymentMethod;
import com.monsoon.seedflowplus.domain.billing.payment.entity.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PaymentResponse {

    private Long paymentId;
    private String paymentCode;
    private Long invoiceId;
    private String invoiceCode;
    private Long clientId;
    private BigDecimal paymentAmount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private LocalDateTime createdAt;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .paymentCode(payment.getPaymentCode())
                .invoiceId(payment.getInvoice() != null ? payment.getInvoice().getId() : null)
                .invoiceCode(payment.getInvoice() != null ? payment.getInvoice().getInvoiceCode() : null)
                .clientId(payment.getClient() != null ? payment.getClient().getId() : null)
                .paymentAmount(payment.getPaymentAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}