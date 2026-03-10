package com.monsoon.seedflowplus.domain.product.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCultivationTime is a Querydsl query type for CultivationTime
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCultivationTime extends EntityPathBase<CultivationTime> {

    private static final long serialVersionUID = -1585561640L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCultivationTime cultivationTime = new QCultivationTime("cultivationTime");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath croppingSystem = createString("croppingSystem");

    public final NumberPath<Integer> harvestingEnd = createNumber("harvestingEnd", Integer.class);

    public final NumberPath<Integer> harvestingStart = createNumber("harvestingStart", Integer.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Integer> plantingEnd = createNumber("plantingEnd", Integer.class);

    public final NumberPath<Integer> plantingStart = createNumber("plantingStart", Integer.class);

    public final QProduct product;

    public final StringPath region = createString("region");

    public final NumberPath<Integer> sowingEnd = createNumber("sowingEnd", Integer.class);

    public final NumberPath<Integer> sowingStart = createNumber("sowingStart", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QCultivationTime(String variable) {
        this(CultivationTime.class, forVariable(variable), INITS);
    }

    public QCultivationTime(Path<? extends CultivationTime> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCultivationTime(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCultivationTime(PathMetadata metadata, PathInits inits) {
        this(CultivationTime.class, metadata, inits);
    }

    public QCultivationTime(Class<? extends CultivationTime> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.product = inits.isInitialized("product") ? new QProduct(forProperty("product")) : null;
    }

}

