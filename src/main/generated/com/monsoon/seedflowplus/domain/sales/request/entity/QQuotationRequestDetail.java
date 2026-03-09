package com.monsoon.seedflowplus.domain.sales.request.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QQuotationRequestDetail is a Querydsl query type for QuotationRequestDetail
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QQuotationRequestDetail extends EntityPathBase<QuotationRequestDetail> {

    private static final long serialVersionUID = -767532235L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QQuotationRequestDetail quotationRequestDetail = new QQuotationRequestDetail("quotationRequestDetail");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseEntity(this);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final com.monsoon.seedflowplus.domain.product.entity.QProduct product;

    public final StringPath productCategory = createString("productCategory");

    public final StringPath productName = createString("productName");

    public final NumberPath<Integer> quantity = createNumber("quantity", Integer.class);

    public final QQuotationRequestHeader quotationRequest;

    public QQuotationRequestDetail(String variable) {
        this(QuotationRequestDetail.class, forVariable(variable), INITS);
    }

    public QQuotationRequestDetail(Path<? extends QuotationRequestDetail> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QQuotationRequestDetail(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QQuotationRequestDetail(PathMetadata metadata, PathInits inits) {
        this(QuotationRequestDetail.class, metadata, inits);
    }

    public QQuotationRequestDetail(Class<? extends QuotationRequestDetail> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.product = inits.isInitialized("product") ? new com.monsoon.seedflowplus.domain.product.entity.QProduct(forProperty("product")) : null;
        this.quotationRequest = inits.isInitialized("quotationRequest") ? new QQuotationRequestHeader(forProperty("quotationRequest"), inits.get("quotationRequest")) : null;
    }

}

