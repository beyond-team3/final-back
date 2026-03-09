package com.monsoon.seedflowplus.domain.billing.invoice.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QInvoiceStatement is a Querydsl query type for InvoiceStatement
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QInvoiceStatement extends EntityPathBase<InvoiceStatement> {

    private static final long serialVersionUID = 760743280L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QInvoiceStatement invoiceStatement = new QInvoiceStatement("invoiceStatement");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseEntity(this);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath included = createBoolean("included");

    public final QInvoice invoice;

    public final com.monsoon.seedflowplus.domain.billing.statement.entity.QStatement statement;

    public QInvoiceStatement(String variable) {
        this(InvoiceStatement.class, forVariable(variable), INITS);
    }

    public QInvoiceStatement(Path<? extends InvoiceStatement> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QInvoiceStatement(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QInvoiceStatement(PathMetadata metadata, PathInits inits) {
        this(InvoiceStatement.class, metadata, inits);
    }

    public QInvoiceStatement(Class<? extends InvoiceStatement> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.invoice = inits.isInitialized("invoice") ? new QInvoice(forProperty("invoice"), inits.get("invoice")) : null;
        this.statement = inits.isInitialized("statement") ? new com.monsoon.seedflowplus.domain.billing.statement.entity.QStatement(forProperty("statement"), inits.get("statement")) : null;
    }

}

