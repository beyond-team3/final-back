package com.monsoon.seedflowplus.domain.schedule.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPersonalSchedule is a Querydsl query type for PersonalSchedule
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPersonalSchedule extends EntityPathBase<PersonalSchedule> {

    private static final long serialVersionUID = -1232864204L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPersonalSchedule personalSchedule = new QPersonalSchedule("personalSchedule");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity(this);

    public final BooleanPath allDay = createBoolean("allDay");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath description = createString("description");

    public final DateTimePath<java.time.LocalDateTime> endAt = createDateTime("endAt", java.time.LocalDateTime.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath isDeleted = createBoolean("isDeleted");

    public final com.monsoon.seedflowplus.domain.account.entity.QUser owner;

    public final DateTimePath<java.time.LocalDateTime> startAt = createDateTime("startAt", java.time.LocalDateTime.class);

    public final EnumPath<ScheduleStatus> status = createEnum("status", ScheduleStatus.class);

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final EnumPath<ScheduleVisibility> visibility = createEnum("visibility", ScheduleVisibility.class);

    public QPersonalSchedule(String variable) {
        this(PersonalSchedule.class, forVariable(variable), INITS);
    }

    public QPersonalSchedule(Path<? extends PersonalSchedule> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPersonalSchedule(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPersonalSchedule(PathMetadata metadata, PathInits inits) {
        this(PersonalSchedule.class, metadata, inits);
    }

    public QPersonalSchedule(Class<? extends PersonalSchedule> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.owner = inits.isInitialized("owner") ? new com.monsoon.seedflowplus.domain.account.entity.QUser(forProperty("owner"), inits.get("owner")) : null;
    }

}

