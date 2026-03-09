package com.monsoon.seedflowplus.domain.sales.quotation.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QQuotationHeader is a Querydsl query type for QuotationHeader
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QQuotationHeader extends EntityPathBase<QuotationHeader> {

    private static final long serialVersionUID = 1835792987L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QQuotationHeader quotationHeader = new QQuotationHeader("quotationHeader");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity(this);

    public final com.monsoon.seedflowplus.domain.account.entity.QEmployee author;

    public final com.monsoon.seedflowplus.domain.account.entity.QClient client;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final com.monsoon.seedflowplus.domain.deal.core.entity.QSalesDeal deal;

    public final DatePath<java.time.LocalDate> expiredDate = createDate("expiredDate", java.time.LocalDate.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final ListPath<QuotationDetail, QQuotationDetail> items = this.<QuotationDetail, QQuotationDetail>createList("items", QuotationDetail.class, QQuotationDetail.class, PathInits.DIRECT2);

    public final StringPath memo = createString("memo");

    public final StringPath quotationCode = createString("quotationCode");

    public final com.monsoon.seedflowplus.domain.sales.request.entity.QQuotationRequestHeader quotationRequest;

    public final EnumPath<QuotationStatus> status = createEnum("status", QuotationStatus.class);

    public final NumberPath<java.math.BigDecimal> totalAmount = createNumber("totalAmount", java.math.BigDecimal.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QQuotationHeader(String variable) {
        this(QuotationHeader.class, forVariable(variable), INITS);
    }

    public QQuotationHeader(Path<? extends QuotationHeader> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QQuotationHeader(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QQuotationHeader(PathMetadata metadata, PathInits inits) {
        this(QuotationHeader.class, metadata, inits);
    }

    public QQuotationHeader(Class<? extends QuotationHeader> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.author = inits.isInitialized("author") ? new com.monsoon.seedflowplus.domain.account.entity.QEmployee(forProperty("author")) : null;
        this.client = inits.isInitialized("client") ? new com.monsoon.seedflowplus.domain.account.entity.QClient(forProperty("client"), inits.get("client")) : null;
        this.deal = inits.isInitialized("deal") ? new com.monsoon.seedflowplus.domain.deal.core.entity.QSalesDeal(forProperty("deal"), inits.get("deal")) : null;
        this.quotationRequest = inits.isInitialized("quotationRequest") ? new com.monsoon.seedflowplus.domain.sales.request.entity.QQuotationRequestHeader(forProperty("quotationRequest"), inits.get("quotationRequest")) : null;
    }

}

