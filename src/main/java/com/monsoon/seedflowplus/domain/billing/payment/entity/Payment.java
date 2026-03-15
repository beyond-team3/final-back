package com.monsoon.seedflowplus.domain.billing.payment.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseCreateEntity;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import com.monsoon.seedflowplus.domain.billing.invoice.entity.Invoice;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@AttributeOverride(name = "id", column = @Column(name = "payment_id"))
@Table(name = "tbl_payment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_code", columnNames = {"payment_code"}),
                @UniqueConstraint(name = "uk_payment_invoice_id", columnNames = {"invoice_id"})
        })
public class Payment extends BaseCreateEntity {

    @Column(name = "payment_code", nullable = false, length = 20)
    private String paymentCode;   // PAY-20260223-001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id", nullable = false)
    private SalesDeal deal;

    @Column(name = "payment_amount")
    private BigDecimal paymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PaymentStatus status;

    public static Payment create(Invoice invoice, Client client, SalesDeal deal, PaymentMethod paymentMethod, String paymentCode) {
        Payment payment = new Payment();
        payment.paymentCode = paymentCode;
        payment.invoice = invoice;
        payment.client = client;
        payment.deal = Objects.requireNonNull(deal, "deal must not be null");
        payment.paymentAmount = invoice.getTotalAmount();
        payment.paymentMethod = paymentMethod;
        payment.status = PaymentStatus.PENDING;
        return payment;
    }

    public void complete() {
        this.status = PaymentStatus.COMPLETED;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

}
