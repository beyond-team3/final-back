package com.monsoon.seedflowplus.domain.account.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QClientCrop is a Querydsl query type for ClientCrop
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QClientCrop extends EntityPathBase<ClientCrop> {

    private static final long serialVersionUID = -898053156L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QClientCrop clientCrop = new QClientCrop("clientCrop");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseEntity(this);

    public final QClient client;

    public final StringPath cropName = createString("cropName");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public QClientCrop(String variable) {
        this(ClientCrop.class, forVariable(variable), INITS);
    }

    public QClientCrop(Path<? extends ClientCrop> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QClientCrop(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QClientCrop(PathMetadata metadata, PathInits inits) {
        this(ClientCrop.class, metadata, inits);
    }

    public QClientCrop(Class<? extends ClientCrop> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.client = inits.isInitialized("client") ? new QClient(forProperty("client"), inits.get("client")) : null;
    }

}

