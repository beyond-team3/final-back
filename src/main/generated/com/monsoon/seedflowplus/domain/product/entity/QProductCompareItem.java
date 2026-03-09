package com.monsoon.seedflowplus.domain.product.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProductCompareItem is a Querydsl query type for ProductCompareItem
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProductCompareItem extends EntityPathBase<ProductCompareItem> {

    private static final long serialVersionUID = 1204386636L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProductCompareItem productCompareItem = new QProductCompareItem("productCompareItem");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QProduct product;

    public final QProductCompare productCompare;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QProductCompareItem(String variable) {
        this(ProductCompareItem.class, forVariable(variable), INITS);
    }

    public QProductCompareItem(Path<? extends ProductCompareItem> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProductCompareItem(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProductCompareItem(PathMetadata metadata, PathInits inits) {
        this(ProductCompareItem.class, metadata, inits);
    }

    public QProductCompareItem(Class<? extends ProductCompareItem> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.product = inits.isInitialized("product") ? new QProduct(forProperty("product")) : null;
        this.productCompare = inits.isInitialized("productCompare") ? new QProductCompare(forProperty("productCompare"), inits.get("productCompare")) : null;
    }

}

