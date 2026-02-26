package com.monsoon.seedflowplus.domain.billing.invoice.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseEntity;
import com.monsoon.seedflowplus.domain.billing.statement.entity.Statement;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@AttributeOverride(name = "id", column = @Column(name = "invoice_statement_id"))
@Table(name = "tbl_invoice_statement",
        uniqueConstraints = @UniqueConstraint(columnNames = {"invoice_id", "statement_id"}))
public class InvoiceStatement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_id", nullable = false)
    private Statement statement;

    // true: 청구서에 포함 / false: 영업사원이 제외 → 다음 주기에 재포함 대상
    @Column(name = "is_included", nullable = false)
    private boolean included;

    // 생성 (기본 포함 상태)
    public static InvoiceStatement create(Invoice invoice, Statement statement) {
        InvoiceStatement is = new InvoiceStatement();
        is.invoice = invoice;
        is.statement = statement;
        is.included = true;
        return is;
    }

    // 영업사원이 제외 처리
    public void exclude() {
        this.included = false;
    }

    // 다시 포함 처리
    public void include() {
        this.included = true;
    }
}