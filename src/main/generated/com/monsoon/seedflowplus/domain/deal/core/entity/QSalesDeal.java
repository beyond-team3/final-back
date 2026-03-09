package com.monsoon.seedflowplus.domain.deal.core.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSalesDeal is a Querydsl query type for SalesDeal
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSalesDeal extends EntityPathBase<SalesDeal> {

    private static final long serialVersionUID = -280660189L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSalesDeal salesDeal = new QSalesDeal("salesDeal");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity(this);

    public final com.monsoon.seedflowplus.domain.account.entity.QClient client;

    public final DateTimePath<java.time.LocalDateTime> closedAt = createDateTime("closedAt", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final EnumPath<com.monsoon.seedflowplus.domain.deal.common.DealStage> currentStage = createEnum("currentStage", com.monsoon.seedflowplus.domain.deal.common.DealStage.class);

    public final StringPath currentStatus = createString("currentStatus");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final DateTimePath<java.time.LocalDateTime> lastActivityAt = createDateTime("lastActivityAt", java.time.LocalDateTime.class);

    public final EnumPath<com.monsoon.seedflowplus.domain.deal.common.DealType> latestDocType = createEnum("latestDocType", com.monsoon.seedflowplus.domain.deal.common.DealType.class);

    public final NumberPath<Long> latestRefId = createNumber("latestRefId", Long.class);

    public final StringPath latestTargetCode = createString("latestTargetCode");

    public final com.monsoon.seedflowplus.domain.account.entity.QEmployee ownerEmp;

    public final StringPath summaryMemo = createString("summaryMemo");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QSalesDeal(String variable) {
        this(SalesDeal.class, forVariable(variable), INITS);
    }

    public QSalesDeal(Path<? extends SalesDeal> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSalesDeal(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSalesDeal(PathMetadata metadata, PathInits inits) {
        this(SalesDeal.class, metadata, inits);
    }

    public QSalesDeal(Class<? extends SalesDeal> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.client = inits.isInitialized("client") ? new com.monsoon.seedflowplus.domain.account.entity.QClient(forProperty("client"), inits.get("client")) : null;
        this.ownerEmp = inits.isInitialized("ownerEmp") ? new com.monsoon.seedflowplus.domain.account.entity.QEmployee(forProperty("ownerEmp")) : null;
    }

}

