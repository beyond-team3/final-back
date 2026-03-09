package com.monsoon.seedflowplus.domain.notification.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QNotificationDelivery is a Querydsl query type for NotificationDelivery
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNotificationDelivery extends EntityPathBase<NotificationDelivery> {

    private static final long serialVersionUID = -1352873328L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QNotificationDelivery notificationDelivery = new QNotificationDelivery("notificationDelivery");

    public final com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity _super = new com.monsoon.seedflowplus.core.common.entity.QBaseModifyEntity(this);

    public final NumberPath<Integer> attemptCount = createNumber("attemptCount", Integer.class);

    public final EnumPath<DeliveryChannel> channel = createEnum("channel", DeliveryChannel.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath failReason = createString("failReason");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final DateTimePath<java.time.LocalDateTime> lastAttemptAt = createDateTime("lastAttemptAt", java.time.LocalDateTime.class);

    public final QNotification notification;

    public final StringPath providerMessageId = createString("providerMessageId");

    public final DateTimePath<java.time.LocalDateTime> scheduledAt = createDateTime("scheduledAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> sentAt = createDateTime("sentAt", java.time.LocalDateTime.class);

    public final EnumPath<DeliveryStatus> status = createEnum("status", DeliveryStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QNotificationDelivery(String variable) {
        this(NotificationDelivery.class, forVariable(variable), INITS);
    }

    public QNotificationDelivery(Path<? extends NotificationDelivery> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QNotificationDelivery(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QNotificationDelivery(PathMetadata metadata, PathInits inits) {
        this(NotificationDelivery.class, metadata, inits);
    }

    public QNotificationDelivery(Class<? extends NotificationDelivery> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.notification = inits.isInitialized("notification") ? new QNotification(forProperty("notification"), inits.get("notification")) : null;
    }

}

