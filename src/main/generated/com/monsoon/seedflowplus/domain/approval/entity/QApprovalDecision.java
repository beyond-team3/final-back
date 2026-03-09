package com.monsoon.seedflowplus.domain.approval.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QApprovalDecision is a Querydsl query type for ApprovalDecision
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QApprovalDecision extends EntityPathBase<ApprovalDecision> {

    private static final long serialVersionUID = 1372042792L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QApprovalDecision approvalDecision = new QApprovalDecision("approvalDecision");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseCreateEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseCreateEntity(this);

    public final QApprovalStep approvalStep;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> decidedAt = createDateTime("decidedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> decidedByUserId = createNumber("decidedByUserId", Long.class);

    public final EnumPath<DecisionType> decision = createEnum("decision", DecisionType.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath reason = createString("reason");

    public QApprovalDecision(String variable) {
        this(ApprovalDecision.class, forVariable(variable), INITS);
    }

    public QApprovalDecision(Path<? extends ApprovalDecision> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QApprovalDecision(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QApprovalDecision(PathMetadata metadata, PathInits inits) {
        this(ApprovalDecision.class, metadata, inits);
    }

    public QApprovalDecision(Class<? extends ApprovalDecision> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.approvalStep = inits.isInitialized("approvalStep") ? new QApprovalStep(forProperty("approvalStep"), inits.get("approvalStep")) : null;
    }

}

