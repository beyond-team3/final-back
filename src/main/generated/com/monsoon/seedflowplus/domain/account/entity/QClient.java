package com.monsoon.seedflowplus.domain.account.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QClient is a Querydsl query type for Client
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QClient extends EntityPathBase<Client> {

    private static final long serialVersionUID = 53351212L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QClient client = new QClient("client");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity(this);

    public final QUser account;

    public final StringPath address = createString("address");

    public final StringPath ceoName = createString("ceoName");

    public final StringPath clientBrn = createString("clientBrn");

    public final StringPath clientCode = createString("clientCode");

    public final StringPath clientName = createString("clientName");

    public final EnumPath<ClientType> clientType = createEnum("clientType", ClientType.class);

    public final StringPath companyPhone = createString("companyPhone");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final ListPath<ClientCrop, QClientCrop> crops = this.<ClientCrop, QClientCrop>createList("crops", ClientCrop.class, QClientCrop.class, PathInits.DIRECT2);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Double> latitude = createNumber("latitude", Double.class);

    public final NumberPath<Double> longitude = createNumber("longitude", Double.class);

    public final StringPath managerEmail = createString("managerEmail");

    public final QEmployee managerEmployee;

    public final StringPath managerName = createString("managerName");

    public final StringPath managerPhone = createString("managerPhone");

    public final NumberPath<java.math.BigDecimal> totalCredit = createNumber("totalCredit", java.math.BigDecimal.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<java.math.BigDecimal> usedCredit = createNumber("usedCredit", java.math.BigDecimal.class);

    public QClient(String variable) {
        this(Client.class, forVariable(variable), INITS);
    }

    public QClient(Path<? extends Client> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QClient(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QClient(PathMetadata metadata, PathInits inits) {
        this(Client.class, metadata, inits);
    }

    public QClient(Class<? extends Client> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.account = inits.isInitialized("account") ? new QUser(forProperty("account"), inits.get("account")) : null;
        this.managerEmployee = inits.isInitialized("managerEmployee") ? new QEmployee(forProperty("managerEmployee")) : null;
    }

}

