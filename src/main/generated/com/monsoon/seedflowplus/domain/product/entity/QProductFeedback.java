package com.monsoon.seedflowplus.domain.product.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProductFeedback is a Querydsl query type for ProductFeedback
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProductFeedback extends EntityPathBase<ProductFeedback> {

    private static final long serialVersionUID = 657137233L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProductFeedback productFeedback = new QProductFeedback("productFeedback");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity(this);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final com.monsoon.seedflowplus.domain.account.entity.QEmployee employee;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QProduct product;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QProductFeedback(String variable) {
        this(ProductFeedback.class, forVariable(variable), INITS);
    }

    public QProductFeedback(Path<? extends ProductFeedback> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProductFeedback(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProductFeedback(PathMetadata metadata, PathInits inits) {
        this(ProductFeedback.class, metadata, inits);
    }

    public QProductFeedback(Class<? extends ProductFeedback> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.employee = inits.isInitialized("employee") ? new com.monsoon.seedflowplus.domain.account.entity.QEmployee(forProperty("employee")) : null;
        this.product = inits.isInitialized("product") ? new QProduct(forProperty("product")) : null;
    }

}

