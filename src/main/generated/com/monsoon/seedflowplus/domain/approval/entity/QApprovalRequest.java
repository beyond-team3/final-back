package com.monsoon.seedflowplus.domain.approval.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QApprovalRequest is a Querydsl query type for ApprovalRequest
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QApprovalRequest extends EntityPathBase<ApprovalRequest> {

    private static final long serialVersionUID = 290419395L;

    public static final QApprovalRequest approvalRequest = new QApprovalRequest("approvalRequest");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity(this);

    public final NumberPath<Long> clientIdSnapshot = createNumber("clientIdSnapshot", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final EnumPath<com.monsoon.seedflowplus.domain.deal.common.DealType> dealType = createEnum("dealType", com.monsoon.seedflowplus.domain.deal.common.DealType.class);

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final EnumPath<ApprovalStatus> status = createEnum("status", ApprovalStatus.class);

    public final ListPath<ApprovalStep, QApprovalStep> steps = this.<ApprovalStep, QApprovalStep>createList("steps", ApprovalStep.class, QApprovalStep.class, PathInits.DIRECT2);

    public final StringPath targetCodeSnapshot = createString("targetCodeSnapshot");

    public final NumberPath<Long> targetId = createNumber("targetId", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QApprovalRequest(String variable) {
        super(ApprovalRequest.class, forVariable(variable));
    }

    public QApprovalRequest(Path<? extends ApprovalRequest> path) {
        super(path.getType(), path.getMetadata());
    }

    public QApprovalRequest(PathMetadata metadata) {
        super(ApprovalRequest.class, metadata);
    }

}

