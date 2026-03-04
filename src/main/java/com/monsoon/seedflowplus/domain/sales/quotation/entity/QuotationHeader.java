package com.monsoon.seedflowplus.domain.sales.quotation.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.sales.request.entity.QuotationRequestHeader;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "id", column = @Column(name = "quo_id"))
@Table(name = "tbl_quotation_header")
public class QuotationHeader extends BaseModifyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id")
    private QuotationRequestHeader quotationRequest; // 참조 견적 요청서

    @Column(name = "quotation_code", unique = true)
    private String quotationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Employee author; // 작성자

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private QuotationStatus status;

    @Column(name = "total_amount")
    private BigDecimal totalAmount; // 견적서에 적힐 총가격

    @Column(name = "expired_date")
    private LocalDate expiredDate; // 만료일

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo; // 문서 비고

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuotationDetail> items = new ArrayList<>();

    private QuotationHeader(QuotationRequestHeader quotationRequest, String quotationCode, Client client,
                            Employee author, BigDecimal totalAmount, String memo) {
        this.quotationRequest = quotationRequest;
        this.quotationCode = quotationCode;
        this.client = client;
        this.author = author;
        this.totalAmount = totalAmount;
        this.memo = memo;
        this.status = QuotationStatus.WAITING_ADMIN;
    }

    public static QuotationHeader create(QuotationRequestHeader quotationRequest, String tempCode, Client client,
                                         Employee author, BigDecimal totalAmount, String memo) {
        return new QuotationHeader(quotationRequest, tempCode, client, author, totalAmount, memo);
    }

    public void updateQuotationCode(String quotationCode) {
        if (quotationCode == null || quotationCode.isBlank()) {
            throw new IllegalArgumentException("quotationCode must not be null or blank");
        }
        this.quotationCode = quotationCode;
    }

    public void updateStatus(QuotationStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        this.status = status;
    }

    public void addItem(QuotationDetail item) {
        if (item == null) {
            throw new IllegalArgumentException("item must not be null");
        }
        this.items.add(item);
        item.setQuotation(this);
    }

    @PrePersist
    public void prePersist() {
        if (this.expiredDate == null) {
            this.expiredDate = LocalDate.now().plusDays(30);
        }
    }

}
