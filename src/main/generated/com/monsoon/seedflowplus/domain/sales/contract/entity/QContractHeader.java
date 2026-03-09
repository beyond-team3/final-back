package com.monsoon.seedflowplus.domain.sales.contract.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QContractHeader is a Querydsl query type for ContractHeader
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QContractHeader extends EntityPathBase<ContractHeader> {

    private static final long serialVersionUID = 254319449L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QContractHeader contractHeader = new QContractHeader("contractHeader");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity(this);

    public final com.monsoon.seedflowplus.domain.account.entity.QEmployee author;

    public final EnumPath<BillingCycle> billingCycle = createEnum("billingCycle", BillingCycle.class);

    public final com.monsoon.seedflowplus.domain.account.entity.QClient client;

    public final StringPath contractCode = createString("contractCode");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final com.monsoon.seedflowplus.domain.deal.core.entity.QSalesDeal deal;

    public final DatePath<java.time.LocalDate> endDate = createDate("endDate", java.time.LocalDate.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final ListPath<ContractDetail, QContractDetail> items = this.<ContractDetail, QContractDetail>createList("items", ContractDetail.class, QContractDetail.class, PathInits.DIRECT2);

    public final StringPath memo = createString("memo");

    public final com.monsoon.seedflowplus.domain.sales.quotation.entity.QQuotationHeader quotation;

    public final StringPath specialTerms = createString("specialTerms");

    public final DatePath<java.time.LocalDate> startDate = createDate("startDate", java.time.LocalDate.class);

    public final EnumPath<ContractStatus> status = createEnum("status", ContractStatus.class);

    public final NumberPath<java.math.BigDecimal> totalAmount = createNumber("totalAmount", java.math.BigDecimal.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QContractHeader(String variable) {
        this(ContractHeader.class, forVariable(variable), INITS);
    }

    public QContractHeader(Path<? extends ContractHeader> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QContractHeader(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QContractHeader(PathMetadata metadata, PathInits inits) {
        this(ContractHeader.class, metadata, inits);
    }

    public QContractHeader(Class<? extends ContractHeader> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.author = inits.isInitialized("author") ? new com.monsoon.seedflowplus.domain.account.entity.QEmployee(forProperty("author")) : null;
        this.client = inits.isInitialized("client") ? new com.monsoon.seedflowplus.domain.account.entity.QClient(forProperty("client"), inits.get("client")) : null;
        this.deal = inits.isInitialized("deal") ? new com.monsoon.seedflowplus.domain.deal.core.entity.QSalesDeal(forProperty("deal"), inits.get("deal")) : null;
        this.quotation = inits.isInitialized("quotation") ? new com.monsoon.seedflowplus.domain.sales.quotation.entity.QQuotationHeader(forProperty("quotation"), inits.get("quotation")) : null;
    }

}

