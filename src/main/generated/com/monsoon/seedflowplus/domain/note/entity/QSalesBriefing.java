package com.monsoon.seedflowplus.domain.note.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSalesBriefing is a Querydsl query type for SalesBriefing
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSalesBriefing extends EntityPathBase<SalesBriefing> {

    private static final long serialVersionUID = 1906824508L;

    public static final QSalesBriefing salesBriefing = new QSalesBriefing("salesBriefing");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity(this);

    public final NumberPath<Long> clientId = createNumber("clientId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final ListPath<Long, NumberPath<Long>> evidenceNoteIds = this.<Long, NumberPath<Long>>createList("evidenceNoteIds", Long.class, NumberPath.class, PathInits.DIRECT2);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final ListPath<String, StringPath> longTermPattern = this.<String, StringPath>createList("longTermPattern", String.class, StringPath.class, PathInits.DIRECT2);

    public final ListPath<String, StringPath> statusChange = this.<String, StringPath>createList("statusChange", String.class, StringPath.class, PathInits.DIRECT2);

    public final StringPath strategySuggestion = createString("strategySuggestion");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath version = createString("version");

    public QSalesBriefing(String variable) {
        super(SalesBriefing.class, forVariable(variable));
    }

    public QSalesBriefing(Path<? extends SalesBriefing> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSalesBriefing(PathMetadata metadata) {
        super(SalesBriefing.class, metadata);
    }

}

