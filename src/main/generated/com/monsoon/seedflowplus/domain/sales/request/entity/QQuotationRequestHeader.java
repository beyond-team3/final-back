package com.monsoon.seedflowplus.domain.sales.request.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QQuotationRequestHeader is a Querydsl query type for QuotationRequestHeader
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QQuotationRequestHeader extends EntityPathBase<QuotationRequestHeader> {

    private static final long serialVersionUID = -653578895L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QQuotationRequestHeader quotationRequestHeader = new QQuotationRequestHeader("quotationRequestHeader");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity(this);

    public final com.monsoon.seedflowplus.domain.account.entity.QClient client;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final com.monsoon.seedflowplus.domain.deal.core.entity.QSalesDeal deal;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final ListPath<QuotationRequestDetail, QQuotationRequestDetail> items = this.<QuotationRequestDetail, QQuotationRequestDetail>createList("items", QuotationRequestDetail.class, QQuotationRequestDetail.class, PathInits.DIRECT2);

    public final StringPath requestCode = createString("requestCode");

    public final StringPath requirements = createString("requirements");

    public final EnumPath<QuotationRequestStatus> status = createEnum("status", QuotationRequestStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QQuotationRequestHeader(String variable) {
        this(QuotationRequestHeader.class, forVariable(variable), INITS);
    }

    public QQuotationRequestHeader(Path<? extends QuotationRequestHeader> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QQuotationRequestHeader(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QQuotationRequestHeader(PathMetadata metadata, PathInits inits) {
        this(QuotationRequestHeader.class, metadata, inits);
    }

    public QQuotationRequestHeader(Class<? extends QuotationRequestHeader> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.client = inits.isInitialized("client") ? new com.monsoon.seedflowplus.domain.account.entity.QClient(forProperty("client"), inits.get("client")) : null;
        this.deal = inits.isInitialized("deal") ? new com.monsoon.seedflowplus.domain.deal.core.entity.QSalesDeal(forProperty("deal"), inits.get("deal")) : null;
    }

}

