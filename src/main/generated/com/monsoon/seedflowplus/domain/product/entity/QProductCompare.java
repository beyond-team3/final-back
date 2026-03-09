package com.monsoon.seedflowplus.domain.product.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProductCompare is a Querydsl query type for ProductCompare
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProductCompare extends EntityPathBase<ProductCompare> {

    private static final long serialVersionUID = 8028313L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProductCompare productCompare = new QProductCompare("productCompare");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity(this);

    public final com.monsoon.seedflowplus.domain.account.entity.QUser account;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final ListPath<ProductCompareItem, QProductCompareItem> items = this.<ProductCompareItem, QProductCompareItem>createList("items", ProductCompareItem.class, QProductCompareItem.class, PathInits.DIRECT2);

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QProductCompare(String variable) {
        this(ProductCompare.class, forVariable(variable), INITS);
    }

    public QProductCompare(Path<? extends ProductCompare> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProductCompare(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProductCompare(PathMetadata metadata, PathInits inits) {
        this(ProductCompare.class, metadata, inits);
    }

    public QProductCompare(Class<? extends ProductCompare> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.account = inits.isInitialized("account") ? new com.monsoon.seedflowplus.domain.account.entity.QUser(forProperty("account"), inits.get("account")) : null;
    }

}

