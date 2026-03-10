package com.monsoon.seedflowplus.domain.product.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProductBookmark is a Querydsl query type for ProductBookmark
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProductBookmark extends EntityPathBase<ProductBookmark> {

    private static final long serialVersionUID = -1440950270L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProductBookmark productBookmark = new QProductBookmark("productBookmark");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseCreateEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseCreateEntity(this);

    public final com.monsoon.seedflowplus.domain.account.entity.QUser account;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QProduct product;

    public QProductBookmark(String variable) {
        this(ProductBookmark.class, forVariable(variable), INITS);
    }

    public QProductBookmark(Path<? extends ProductBookmark> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProductBookmark(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProductBookmark(PathMetadata metadata, PathInits inits) {
        this(ProductBookmark.class, metadata, inits);
    }

    public QProductBookmark(Class<? extends ProductBookmark> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.account = inits.isInitialized("account") ? new com.monsoon.seedflowplus.domain.account.entity.QUser(forProperty("account"), inits.get("account")) : null;
        this.product = inits.isInitialized("product") ? new QProduct(forProperty("product")) : null;
    }

}

