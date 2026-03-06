package com.monsoon.seedflowplus.domain.schedule.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import com.monsoon.seedflowplus.domain.account.entity.User;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@AttributeOverride(name = "id", column = @Column(name = "psked_id"))
@Table(
        name = "tbl_pers_sked",
        indexes = {
                @Index(name = "idx_pers_sked_owner_start_at", columnList = "owner_id, start_at"),
                @Index(name = "idx_pers_sked_owner_end_at", columnList = "owner_id, end_at")
        }
)
public class PersonalSchedule extends BaseModifyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "all_day", nullable = false)
    private boolean allDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScheduleStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private ScheduleVisibility visibility;

    @Builder
    public PersonalSchedule(
            User owner,
            String title,
            String description,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean allDay,
            ScheduleStatus status,
            ScheduleVisibility visibility
    ) {
        validate(owner, title, startAt, endAt, status, visibility);
        this.owner = owner;
        this.title = title.trim();
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
        this.allDay = allDay;
        this.status = status;
        this.visibility = visibility;
    }

    public void update(
            String title,
            String description,
            LocalDateTime startAt,
            LocalDateTime endAt,
            boolean allDay,
            ScheduleStatus status,
            ScheduleVisibility visibility
    ) {
        validate(this.owner, title, startAt, endAt, status, visibility);
        this.title = title.trim();
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
        this.allDay = allDay;
        this.status = status;
        this.visibility = visibility;
    }

    public void cancel() {
        this.status = ScheduleStatus.CANCELED;
    }

    private void validate(
            User owner,
            String title,
            LocalDateTime startAt,
            LocalDateTime endAt,
            ScheduleStatus status,
            ScheduleVisibility visibility
    ) {
        if (owner == null) {
            throw new IllegalArgumentException("owner must not be null");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("startAt/endAt must not be null");
        }
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (visibility == null) {
            throw new IllegalArgumentException("visibility must not be null");
        }
    }
}
