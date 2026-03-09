package com.monsoon.seedflowplus.domain.scoring.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAccountScore is a Querydsl query type for AccountScore
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAccountScore extends EntityPathBase<AccountScore> {

    private static final long serialVersionUID = -796575064L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAccountScore accountScore = new QAccountScore("accountScore");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity(this);

    public final com.monsoon.seedflowplus.domain.account.entity.QClient client;

    public final NumberPath<Double> contractScore = createNumber("contractScore", Double.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath detailDescription = createString("detailDescription");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Double> orderScore = createNumber("orderScore", Double.class);

    public final StringPath primaryReason = createString("primaryReason");

    public final NumberPath<Double> totalScore = createNumber("totalScore", Double.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Double> visitScore = createNumber("visitScore", Double.class);

    public QAccountScore(String variable) {
        this(AccountScore.class, forVariable(variable), INITS);
    }

    public QAccountScore(Path<? extends AccountScore> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAccountScore(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAccountScore(PathMetadata metadata, PathInits inits) {
        this(AccountScore.class, metadata, inits);
    }

    public QAccountScore(Class<? extends AccountScore> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.client = inits.isInitialized("client") ? new com.monsoon.seedflowplus.domain.account.entity.QClient(forProperty("client"), inits.get("client")) : null;
    }

}

