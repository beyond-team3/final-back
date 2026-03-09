package com.monsoon.seedflowplus.domain.product.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProductPriceHistory is a Querydsl query type for ProductPriceHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProductPriceHistory extends EntityPathBase<ProductPriceHistory> {

    private static final long serialVersionUID = 1985766359L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProductPriceHistory productPriceHistory = new QProductPriceHistory("productPriceHistory");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseCreateEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseCreateEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final com.monsoon.seedflowplus.domain.account.entity.QEmployee modifiedBy;

    public final NumberPath<java.math.BigDecimal> newPrice = createNumber("newPrice", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> oldPrice = createNumber("oldPrice", java.math.BigDecimal.class);

    public final QProduct product;

    public QProductPriceHistory(String variable) {
        this(ProductPriceHistory.class, forVariable(variable), INITS);
    }

    public QProductPriceHistory(Path<? extends ProductPriceHistory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProductPriceHistory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProductPriceHistory(PathMetadata metadata, PathInits inits) {
        this(ProductPriceHistory.class, metadata, inits);
    }

    public QProductPriceHistory(Class<? extends ProductPriceHistory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.modifiedBy = inits.isInitialized("modifiedBy") ? new com.monsoon.seedflowplus.domain.account.entity.QEmployee(forProperty("modifiedBy")) : null;
        this.product = inits.isInitialized("product") ? new QProduct(forProperty("product")) : null;
    }

}

