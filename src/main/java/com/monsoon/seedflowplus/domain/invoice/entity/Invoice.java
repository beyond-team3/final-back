package com.monsoon.seedflowplus.domain.invoice.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseCreateEntity;
import com.monsoon.seedflowplus.domain.account.Client;
import com.monsoon.seedflowplus.domain.account.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@AttributeOverride(name = "id", column = @Column(name = "invoice_id"))
@Table(name = "tbl_invoice")
public class Invoice extends BaseCreateEntity {
    @Column(name = "invoice_code", nullable = false, unique = true, length = 20)
    private String invoiceCode;   // INV-20260223-001

    @Column(name = "contract_id", nullable = false)
    private Long contractId;      // 타 파트라 ID만 저장

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", referencedColumnName = "id")
    private Employee employee;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "supply_amount")
    private BigDecimal supplyAmount;

    @Column(name = "vat_amount")
    private BigDecimal vatAmount;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status;

    @Column(name = "note")
    private String note;

    // 생성
    public static Invoice create(Long contractId, Client client, Employee employee,
                                 LocalDate invoiceDate, LocalDate startDate, LocalDate endDate,
                                 String invoiceCode, String note) {
        Invoice invoice = new Invoice();
        invoice.invoiceCode = invoiceCode;
        invoice.contractId = contractId;
        invoice.client = client;
        invoice.employee = employee;
        invoice.invoiceDate = invoiceDate;
        invoice.startDate = startDate;
        invoice.endDate = endDate;
        invoice.supplyAmount = BigDecimal.ZERO;
        invoice.vatAmount = BigDecimal.ZERO;
        invoice.totalAmount = BigDecimal.ZERO;
        invoice.status = InvoiceStatus.DRAFT;
        invoice.note = note;
        return invoice;
    }

    // 금액 업데이트
    public void updateAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
        this.supplyAmount = totalAmount.divide(BigDecimal.valueOf(1.1), 2, RoundingMode.HALF_UP);
        this.vatAmount = totalAmount.subtract(this.supplyAmount);
    }

    // 발행 확정
    public void publish() {
        this.status = InvoiceStatus.PUBLISHED;
    }

    // 결제 완료
    public void paid() {
        this.status = InvoiceStatus.PAID;
    }

    // 취소
    public void cancel() {
        this.status = InvoiceStatus.CANCELED;
    }
}
