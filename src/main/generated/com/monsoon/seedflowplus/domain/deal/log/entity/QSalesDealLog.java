package com.monsoon.seedflowplus.domain.deal.log.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSalesDealLog is a Querydsl query type for SalesDealLog
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSalesDealLog extends EntityPathBase<SalesDealLog> {

    private static final long serialVersionUID = 345797364L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSalesDealLog salesDealLog = new QSalesDealLog("salesDealLog");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseCreateEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseCreateEntity(this);

    public final DateTimePath<java.time.LocalDateTime> actionAt = createDateTime("actionAt", java.time.LocalDateTime.class);

    public final EnumPath<com.monsoon.seedflowplus.domain.deal.common.ActionType> actionType = createEnum("actionType", com.monsoon.seedflowplus.domain.deal.common.ActionType.class);

    public final NumberPath<Long> actorId = createNumber("actorId", Long.class);

    public final EnumPath<com.monsoon.seedflowplus.domain.deal.common.ActorType> actorType = createEnum("actorType", com.monsoon.seedflowplus.domain.deal.common.ActorType.class);

    public final com.monsoon.seedflowplus.domain.account.entity.QClient client;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final com.monsoon.seedflowplus.domain.deal.core.entity.QSalesDeal deal;

    public final QDealLogDetail detail;

    public final EnumPath<com.monsoon.seedflowplus.domain.deal.common.DealType> docType = createEnum("docType", com.monsoon.seedflowplus.domain.deal.common.DealType.class);

    public final EnumPath<com.monsoon.seedflowplus.domain.deal.common.DealStage> fromStage = createEnum("fromStage", com.monsoon.seedflowplus.domain.deal.common.DealStage.class);

    public final StringPath fromStatus = createString("fromStatus");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Long> refId = createNumber("refId", Long.class);

    public final StringPath targetCode = createString("targetCode");

    public final EnumPath<com.monsoon.seedflowplus.domain.deal.common.DealStage> toStage = createEnum("toStage", com.monsoon.seedflowplus.domain.deal.common.DealStage.class);

    public final StringPath toStatus = createString("toStatus");

    public QSalesDealLog(String variable) {
        this(SalesDealLog.class, forVariable(variable), INITS);
    }

    public QSalesDealLog(Path<? extends SalesDealLog> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSalesDealLog(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSalesDealLog(PathMetadata metadata, PathInits inits) {
        this(SalesDealLog.class, metadata, inits);
    }

    public QSalesDealLog(Class<? extends SalesDealLog> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.client = inits.isInitialized("client") ? new com.monsoon.seedflowplus.domain.account.entity.QClient(forProperty("client"), inits.get("client")) : null;
        this.deal = inits.isInitialized("deal") ? new com.monsoon.seedflowplus.domain.deal.core.entity.QSalesDeal(forProperty("deal"), inits.get("deal")) : null;
        this.detail = inits.isInitialized("detail") ? new QDealLogDetail(forProperty("detail"), inits.get("detail")) : null;
    }

}

