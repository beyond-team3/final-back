package com.monsoon.seedflowplus.domain.sales.contract.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QContractDetail is a Querydsl query type for ContractDetail
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QContractDetail extends EntityPathBase<ContractDetail> {

    private static final long serialVersionUID = 140366109L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QContractDetail contractDetail = new QContractDetail("contractDetail");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseEntity(this);

    public final NumberPath<java.math.BigDecimal> amount = createNumber("amount", java.math.BigDecimal.class);

    public final QContractHeader contract;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final com.monsoon.seedflowplus.domain.product.entity.QProduct product;

    public final StringPath productCategory = createString("productCategory");

    public final StringPath productName = createString("productName");

    public final NumberPath<Integer> totalQuantity = createNumber("totalQuantity", Integer.class);

    public final StringPath unit = createString("unit");

    public final NumberPath<java.math.BigDecimal> unitPrice = createNumber("unitPrice", java.math.BigDecimal.class);

    public QContractDetail(String variable) {
        this(ContractDetail.class, forVariable(variable), INITS);
    }

    public QContractDetail(Path<? extends ContractDetail> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QContractDetail(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QContractDetail(PathMetadata metadata, PathInits inits) {
        this(ContractDetail.class, metadata, inits);
    }

    public QContractDetail(Class<? extends ContractDetail> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contract = inits.isInitialized("contract") ? new QContractHeader(forProperty("contract"), inits.get("contract")) : null;
        this.product = inits.isInitialized("product") ? new com.monsoon.seedflowplus.domain.product.entity.QProduct(forProperty("product")) : null;
    }

}

