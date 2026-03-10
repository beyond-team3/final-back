package com.monsoon.seedflowplus.core.common.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QBaseCreateEntity is a Querydsl query type for BaseCreateEntity
 */
@Generated("com.querydsl.codegen.DefaultSupertypeSerializer")
public class QBaseCreateEntity extends EntityPathBase<BaseCreateEntity> {

    private static final long serialVersionUID = -1942475834L;

    public static final QBaseCreateEntity baseCreateEntity = new QBaseCreateEntity("baseCreateEntity");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public QBaseCreateEntity(String variable) {
        super(BaseCreateEntity.class, forVariable(variable));
    }

    public QBaseCreateEntity(Path<? extends BaseCreateEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QBaseCreateEntity(PathMetadata metadata) {
        super(BaseCreateEntity.class, metadata);
    }

}

