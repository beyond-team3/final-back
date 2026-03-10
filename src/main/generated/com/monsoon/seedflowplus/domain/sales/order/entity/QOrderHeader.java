package com.monsoon.seedflowplus.domain.sales.order.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrderHeader is a Querydsl query type for OrderHeader
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrderHeader extends EntityPathBase<OrderHeader> {

    private static final long serialVersionUID = -29907941L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrderHeader orderHeader = new QOrderHeader("orderHeader");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseCreateEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseCreateEntity(this);

    public final com.monsoon.seedflowplus.domain.account.entity.QClient client;

    public final com.monsoon.seedflowplus.domain.sales.contract.entity.QContractHeader contract;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final com.monsoon.seedflowplus.domain.deal.core.entity.QSalesDeal deal;

    public final com.monsoon.seedflowplus.domain.account.entity.QEmployee employee;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath orderCode = createString("orderCode");

    public final EnumPath<OrderStatus> status = createEnum("status", OrderStatus.class);

    public final NumberPath<java.math.BigDecimal> totalAmount = createNumber("totalAmount", java.math.BigDecimal.class);

    public QOrderHeader(String variable) {
        this(OrderHeader.class, forVariable(variable), INITS);
    }

    public QOrderHeader(Path<? extends OrderHeader> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrderHeader(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrderHeader(PathMetadata metadata, PathInits inits) {
        this(OrderHeader.class, metadata, inits);
    }

    public QOrderHeader(Class<? extends OrderHeader> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.client = inits.isInitialized("client") ? new com.monsoon.seedflowplus.domain.account.entity.QClient(forProperty("client"), inits.get("client")) : null;
        this.contract = inits.isInitialized("contract") ? new com.monsoon.seedflowplus.domain.sales.contract.entity.QContractHeader(forProperty("contract"), inits.get("contract")) : null;
        this.deal = inits.isInitialized("deal") ? new com.monsoon.seedflowplus.domain.deal.core.entity.QSalesDeal(forProperty("deal"), inits.get("deal")) : null;
        this.employee = inits.isInitialized("employee") ? new com.monsoon.seedflowplus.domain.account.entity.QEmployee(forProperty("employee")) : null;
    }

}

