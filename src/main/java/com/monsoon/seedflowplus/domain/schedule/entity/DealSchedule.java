package com.monsoon.seedflowplus.domain.schedule.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import com.monsoon.seedflowplus.domain.account.entity.Client;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.deal.core.entity.SalesDeal;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@AttributeOverride(name = "id", column = @Column(name = "deal_sked_id"))
@Table(
        name = "tbl_deal_sked",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_deal_sked_external_key", columnNames = "external_key")
        },
        indexes = {
                @Index(name = "idx_deal_sked_assignee_start_end", columnList = "assignee_user_id, start_at, end_at"),
                @Index(name = "idx_deal_sked_client_start_end", columnList = "client_id, start_at, end_at"),
                @Index(name = "idx_deal_sked_deal_start_end", columnList = "deal_id, start_at, end_at"),
                @Index(name = "idx_deal_sked_start_end", columnList = "start_at, end_at")
        }
)
public class DealSchedule extends BaseModifyEntity {

    private static final int TITLE_MAX_LENGTH = 200;
    private static final int EXTERNAL_KEY_MAX_LENGTH = 180;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_id", nullable = false)
    private SalesDeal deal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_user_id", nullable = false)
    private User assigneeUser;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private DealScheduleEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false, length = 20)
    private DealDocType docType;

    @Column(name = "ref_doc_id")
    private Long refDocId;

    @Column(name = "ref_deal_log_id")
    private Long refDealLogId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private ScheduleSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DealScheduleStatus status;

    @Column(name = "external_key", nullable = false, length = 180)
    private String externalKey;

    @Column(name = "last_synced_at", nullable = false)
    private LocalDateTime lastSyncedAt;

    @Builder
    public DealSchedule(
            SalesDeal deal,
            Client client,
            User assigneeUser,
            String title,
            String description,
            LocalDateTime startAt,
            LocalDateTime endAt,
            DealScheduleEventType eventType,
            DealDocType docType,
            Long refDocId,
            Long refDealLogId,
            ScheduleSource source,
            DealScheduleStatus status,
            String externalKey,
            LocalDateTime lastSyncedAt
    ) {
        validate(deal, client, assigneeUser, title, startAt, endAt, eventType, docType, source, status, externalKey, lastSyncedAt);
        this.deal = deal;
        this.client = client;
        this.assigneeUser = assigneeUser;
        this.title = title.trim();
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
        this.eventType = eventType;
        this.docType = docType;
        this.refDocId = refDocId;
        this.refDealLogId = refDealLogId;
        this.source = source;
        this.status = status;
        this.externalKey = externalKey == null ? null : externalKey.trim();
        this.lastSyncedAt = lastSyncedAt;
    }

    public void syncUpdate(
            User assigneeUser,
            String title,
            String description,
            LocalDateTime startAt,
            LocalDateTime endAt,
            DealScheduleEventType eventType,
            DealDocType docType,
            Long refDocId,
            Long refDealLogId,
            ScheduleSource source,
            DealScheduleStatus status,
            LocalDateTime lastSyncedAt
    ) {
        validate(this.deal, this.client, assigneeUser, title, startAt, endAt, eventType, docType, source, status, this.externalKey, lastSyncedAt);
        this.assigneeUser = assigneeUser;
        this.title = title.trim();
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
        this.eventType = eventType;
        this.docType = docType;
        this.refDocId = refDocId;
        this.refDealLogId = refDealLogId;
        this.source = source;
        this.status = status;
        this.lastSyncedAt = lastSyncedAt;
    }

    public void cancel(LocalDateTime lastSyncedAt) {
        if (lastSyncedAt == null) {
            throw new IllegalArgumentException("lastSyncedAt");
        }
        this.status = DealScheduleStatus.CANCELLED;
        this.lastSyncedAt = lastSyncedAt;
    }

    private void validate(
            SalesDeal deal,
            Client client,
            User assigneeUser,
            String title,
            LocalDateTime startAt,
            LocalDateTime endAt,
            DealScheduleEventType eventType,
            DealDocType docType,
            ScheduleSource source,
            DealScheduleStatus status,
            String externalKey,
            LocalDateTime lastSyncedAt
    ) {
        if (deal == null) {
            throw new IllegalArgumentException("deal");
        }
        if (client == null) {
            throw new IllegalArgumentException("client");
        }
        if (assigneeUser == null) {
            throw new IllegalArgumentException("assigneeUser");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("title");
        }
        if (title.trim().length() > TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException("title");
        }
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("startAt|endAt");
        }
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("startAt|endAt");
        }
        if (eventType == null) {
            throw new IllegalArgumentException("eventType");
        }
        if (docType == null) {
            throw new IllegalArgumentException("docType");
        }
        if (source == null) {
            throw new IllegalArgumentException("source");
        }
        if (status == null) {
            throw new IllegalArgumentException("status");
        }
        if (externalKey == null || externalKey.isBlank()) {
            throw new IllegalArgumentException("externalKey");
        }
        if (externalKey.trim().length() > EXTERNAL_KEY_MAX_LENGTH) {
            throw new IllegalArgumentException("externalKey");
        }
        if (lastSyncedAt == null) {
            throw new IllegalArgumentException("lastSyncedAt");
        }
    }
}
