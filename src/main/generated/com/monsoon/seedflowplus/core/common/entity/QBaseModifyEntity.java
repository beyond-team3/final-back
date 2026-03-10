package com.monsoon.seedflowplus.core.common.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBaseModifyEntity is a Querydsl query type for BaseModifyEntity
 */
@Generated("com.querydsl.codegen.DefaultSupertypeSerializer")
public class QBaseModifyEntity extends EntityPathBase<BaseModifyEntity> {

    private static final long serialVersionUID = 1006681348L;

    public static final QBaseModifyEntity baseModifyEntity = new QBaseModifyEntity("baseModifyEntity");

    public final QBaseCreateEntity _super = new QBaseCreateEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QBaseModifyEntity(String variable) {
        super(BaseModifyEntity.class, forVariable(variable));
    }

    public QBaseModifyEntity(Path<? extends BaseModifyEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBaseModifyEntity(PathMetadata metadata) {
        super(BaseModifyEntity.class, metadata);
    }

}

