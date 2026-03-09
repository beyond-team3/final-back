package com.monsoon.seedflowplus.domain.billing.statement.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QStatement is a Querydsl query type for Statement
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QStatement extends EntityPathBase<Statement> {

    private static final long serialVersionUID = 2073826175L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QStatement statement = new QStatement("statement");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseCreateEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseCreateEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final com.monsoon.seedflowplus.domain.deal.core.entity.QSalesDeal deal;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final com.monsoon.seedflowplus.domain.sales.order.entity.QOrderHeader orderHeader;

    public final StringPath statementCode = createString("statementCode");

    public final EnumPath<StatementStatus> status = createEnum("status", StatementStatus.class);

    public final NumberPath<java.math.BigDecimal> supplyAmount = createNumber("supplyAmount", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> totalAmount = createNumber("totalAmount", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> vatAmount = createNumber("vatAmount", java.math.BigDecimal.class);

    public QStatement(String variable) {
        this(Statement.class, forVariable(variable), INITS);
    }

    public QStatement(Path<? extends Statement> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QStatement(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QStatement(PathMetadata metadata, PathInits inits) {
        this(Statement.class, metadata, inits);
    }

    public QStatement(Class<? extends Statement> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.deal = inits.isInitialized("deal") ? new com.monsoon.seedflowplus.domain.deal.core.entity.QSalesDeal(forProperty("deal"), inits.get("deal")) : null;
        this.orderHeader = inits.isInitialized("orderHeader") ? new com.monsoon.seedflowplus.domain.sales.order.entity.QOrderHeader(forProperty("orderHeader"), inits.get("orderHeader")) : null;
    }

}

