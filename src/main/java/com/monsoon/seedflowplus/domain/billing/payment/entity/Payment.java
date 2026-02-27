package com.monsoon.seedflowplus.domain.billing.payment.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseCreateEntity;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.Invoice;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@AttributeOverride(name = "id", column = @Column(name = "payment_id"))
@Table(name = "tbl_payment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"invoice_id"}))
public class Payment extends BaseCreateEntity {

    @Column(name = "payment_code", nullable = false, unique = true, length = 20)
    private String paymentCode;   // PAY-20260223-001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(name = "payment_amount")
    private BigDecimal paymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PaymentStatus status;

    public static Payment create(Invoice invoice, Client client, PaymentMethod paymentMethod, String paymentCode) {
        Payment payment = new Payment();
        payment.paymentCode = paymentCode;
        payment.invoice = invoice;
        payment.client = client;
        payment.paymentAmount = invoice.getTotalAmount();
        payment.paymentMethod = paymentMethod;
        payment.status = PaymentStatus.COMPLETED;
        return payment;
    }
}