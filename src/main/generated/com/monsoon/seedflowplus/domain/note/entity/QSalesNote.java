package com.monsoon.seedflowplus.domain.note.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSalesNote is a Querydsl query type for SalesNote
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSalesNote extends EntityPathBase<SalesNote> {

    private static final long serialVersionUID = -1957099002L;

    public static final QSalesNote salesNote = new QSalesNote("salesNote");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity(this);

    public final DatePath<java.time.LocalDate> activityDate = createDate("activityDate", java.time.LocalDate.class);

    public final ListPath<String, StringPath> aiSummary = this.<String, StringPath>createList("aiSummary", String.class, StringPath.class, PathInits.DIRECT2);

    public final NumberPath<Long> authorId = createNumber("authorId", Long.class);

    public final NumberPath<Long> clientId = createNumber("clientId", Long.class);

    public final StringPath content = createString("content");

    public final StringPath contractId = createString("contractId");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final BooleanPath isEdited = createBoolean("isEdited");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QSalesNote(String variable) {
        super(SalesNote.class, forVariable(variable));
    }

    public QSalesNote(Path<? extends SalesNote> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSalesNote(PathMetadata metadata) {
        super(SalesNote.class, metadata);
    }

}

