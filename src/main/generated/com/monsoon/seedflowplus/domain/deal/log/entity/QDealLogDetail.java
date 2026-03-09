package com.monsoon.seedflowplus.domain.deal.log.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDealLogDetail is a Querydsl query type for DealLogDetail
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDealLogDetail extends EntityPathBase<DealLogDetail> {

    private static final long serialVersionUID = 10382401L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QDealLogDetail dealLogDetail = new QDealLogDetail("dealLogDetail");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseCreateEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseCreateEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final QSalesDealLog dealLog;

    public final StringPath diffJson = createString("diffJson");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath reason = createString("reason");

    public QDealLogDetail(String variable) {
        this(DealLogDetail.class, forVariable(variable), INITS);
    }

    public QDealLogDetail(Path<? extends DealLogDetail> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QDealLogDetail(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QDealLogDetail(PathMetadata metadata, PathInits inits) {
        this(DealLogDetail.class, metadata, inits);
    }

    public QDealLogDetail(Class<? extends DealLogDetail> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.dealLog = inits.isInitialized("dealLog") ? new QSalesDealLog(forProperty("dealLog"), inits.get("dealLog")) : null;
    }

}

