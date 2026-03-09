package com.monsoon.seedflowplus.domain.sales.order.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QOrderDetail is a Querydsl query type for OrderDetail
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOrderDetail extends EntityPathBase<OrderDetail> {

    private static final long serialVersionUID = -143861281L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QOrderDetail orderDetail = new QOrderDetail("orderDetail");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseEntity(this);

    public final com.monsoon.seedflowplus.domain.sales.contract.entity.QContractDetail contractDetail;

    public final StringPath deliveryRequest = createString("deliveryRequest");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final QOrderHeader orderHeader;

    public final NumberPath<Long> quantity = createNumber("quantity", Long.class);

    public final StringPath shippingAddress = createString("shippingAddress");

    public final StringPath shippingAddressDetail = createString("shippingAddressDetail");

    public final StringPath shippingName = createString("shippingName");

    public final StringPath shippingPhone = createString("shippingPhone");

    public QOrderDetail(String variable) {
        this(OrderDetail.class, forVariable(variable), INITS);
    }

    public QOrderDetail(Path<? extends OrderDetail> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QOrderDetail(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QOrderDetail(PathMetadata metadata, PathInits inits) {
        this(OrderDetail.class, metadata, inits);
    }

    public QOrderDetail(Class<? extends OrderDetail> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contractDetail = inits.isInitialized("contractDetail") ? new com.monsoon.seedflowplus.domain.sales.contract.entity.QContractDetail(forProperty("contractDetail"), inits.get("contractDetail")) : null;
        this.orderHeader = inits.isInitialized("orderHeader") ? new QOrderHeader(forProperty("orderHeader"), inits.get("orderHeader")) : null;
    }

}

