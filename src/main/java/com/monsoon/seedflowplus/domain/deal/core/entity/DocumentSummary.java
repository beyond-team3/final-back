package com.monsoon.seedflowplus.domain.deal.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

@Getter
@Entity
@Immutable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Subselect("""
    SELECT surrogate_id, doc_type, doc_id, deal_id, client_id,
           doc_code, amount, expired_date, status, created_at
    FROM v_document_summary
    """)
public class DocumentSummary {

    @Id
    @Column(name = "surrogate_id")
    private String surrogateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type")
    private DealType docType;

    @Column(name = "doc_id")
    private Long docId;

    @Column(name = "deal_id")
    private Long dealId;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "doc_code")
    private String docCode;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "expired_date")
    private LocalDate expiredDate;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
