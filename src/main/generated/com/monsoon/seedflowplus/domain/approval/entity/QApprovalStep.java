package com.monsoon.seedflowplus.domain.approval.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QApprovalStep is a Querydsl query type for ApprovalStep
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QApprovalStep extends EntityPathBase<ApprovalStep> {

    private static final long serialVersionUID = -365128936L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QApprovalStep approvalStep = new QApprovalStep("approvalStep");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity(this);

    public final EnumPath<com.monsoon.seedflowplus.domain.deal.common.ActorType> actorType = createEnum("actorType", com.monsoon.seedflowplus.domain.deal.common.ActorType.class);

    public final QApprovalRequest approvalRequest;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> decidedAt = createDateTime("decidedAt", java.time.LocalDateTime.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final EnumPath<ApprovalStepStatus> status = createEnum("status", ApprovalStepStatus.class);

    public final NumberPath<Integer> stepOrder = createNumber("stepOrder", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QApprovalStep(String variable) {
        this(ApprovalStep.class, forVariable(variable), INITS);
    }

    public QApprovalStep(Path<? extends ApprovalStep> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QApprovalStep(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QApprovalStep(PathMetadata metadata, PathInits inits) {
        this(ApprovalStep.class, metadata, inits);
    }

    public QApprovalStep(Class<? extends ApprovalStep> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.approvalRequest = inits.isInitialized("approvalRequest") ? new QApprovalRequest(forProperty("approvalRequest")) : null;
    }

}

