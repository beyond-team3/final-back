package com.monsoon.seedflowplus.erd;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tbl_invoice_statement")
public class InvoiceStatementErd {

    @Id
    @Column(name = "statement_id", length = 10)
    private String statementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_id", insertable = false, updatable = false)
    private StatementErd statement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private InvoiceErd invoice;
}
