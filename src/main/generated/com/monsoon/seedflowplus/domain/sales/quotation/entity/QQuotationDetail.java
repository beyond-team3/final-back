package com.monsoon.seedflowplus.domain.sales.quotation.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QQuotationDetail is a Querydsl query type for QuotationDetail
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QQuotationDetail extends EntityPathBase<QuotationDetail> {

    private static final long serialVersionUID = 1721839647L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QQuotationDetail quotationDetail = new QQuotationDetail("quotationDetail");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseEntity(this);

    public final NumberPath<java.math.BigDecimal> amount = createNumber("amount", java.math.BigDecimal.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final com.monsoon.seedflowplus.domain.product.entity.QProduct product;

    public final StringPath productCategory = createString("productCategory");

    public final StringPath productName = createString("productName");

    public final NumberPath<Integer> quantity = createNumber("quantity", Integer.class);

    public final QQuotationHeader quotation;

    public final StringPath unit = createString("unit");

    public final NumberPath<java.math.BigDecimal> unitPrice = createNumber("unitPrice", java.math.BigDecimal.class);

    public QQuotationDetail(String variable) {
        this(QuotationDetail.class, forVariable(variable), INITS);
    }

    public QQuotationDetail(Path<? extends QuotationDetail> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QQuotationDetail(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QQuotationDetail(PathMetadata metadata, PathInits inits) {
        this(QuotationDetail.class, metadata, inits);
    }

    public QQuotationDetail(Class<? extends QuotationDetail> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.product = inits.isInitialized("product") ? new com.monsoon.seedflowplus.domain.product.entity.QProduct(forProperty("product")) : null;
        this.quotation = inits.isInitialized("quotation") ? new QQuotationHeader(forProperty("quotation"), inits.get("quotation")) : null;
    }

}

