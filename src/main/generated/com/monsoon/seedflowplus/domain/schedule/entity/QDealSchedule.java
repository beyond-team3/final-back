package com.monsoon.seedflowplus.domain.schedule.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDealSchedule is a Querydsl query type for DealSchedule
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDealSchedule extends EntityPathBase<DealSchedule> {

    private static final long serialVersionUID = 622398688L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QDealSchedule dealSchedule = new QDealSchedule("dealSchedule");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity(this);

    public final com.monsoon.seedflowplus.domain.account.entity.QUser assigneeUser;

    public final com.monsoon.seedflowplus.domain.account.entity.QClient client;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final com.monsoon.seedflowplus.domain.deal.core.entity.QSalesDeal deal;

    public final StringPath description = createString("description");

    public final EnumPath<DealDocType> docType = createEnum("docType", DealDocType.class);

    public final DateTimePath<java.time.LocalDateTime> endAt = createDateTime("endAt", java.time.LocalDateTime.class);

    public final EnumPath<DealScheduleEventType> eventType = createEnum("eventType", DealScheduleEventType.class);

    public final StringPath externalKey = createString("externalKey");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final DateTimePath<java.time.LocalDateTime> lastSyncedAt = createDateTime("lastSyncedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> refDealLogId = createNumber("refDealLogId", Long.class);

    public final NumberPath<Long> refDocId = createNumber("refDocId", Long.class);

    public final EnumPath<ScheduleSource> source = createEnum("source", ScheduleSource.class);

    public final DateTimePath<java.time.LocalDateTime> startAt = createDateTime("startAt", java.time.LocalDateTime.class);

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QDealSchedule(String variable) {
        this(DealSchedule.class, forVariable(variable), INITS);
    }

    public QDealSchedule(Path<? extends DealSchedule> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QDealSchedule(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QDealSchedule(PathMetadata metadata, PathInits inits) {
        this(DealSchedule.class, metadata, inits);
    }

    public QDealSchedule(Class<? extends DealSchedule> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.assigneeUser = inits.isInitialized("assigneeUser") ? new com.monsoon.seedflowplus.domain.account.entity.QUser(forProperty("assigneeUser"), inits.get("assigneeUser")) : null;
        this.client = inits.isInitialized("client") ? new com.monsoon.seedflowplus.domain.account.entity.QClient(forProperty("client"), inits.get("client")) : null;
        this.deal = inits.isInitialized("deal") ? new com.monsoon.seedflowplus.domain.deal.core.entity.QSalesDeal(forProperty("deal"), inits.get("deal")) : null;
    }

}

