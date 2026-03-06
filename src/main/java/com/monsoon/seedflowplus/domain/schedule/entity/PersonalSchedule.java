package com.monsoon.seedflowplus.domain.schedule.entity;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
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
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@AttributeOverride(name = "id", column = @Column(name = "personal_schedule_id"))
@SQLDelete(sql = "UPDATE tbl_pers_sked SET is_deleted = true WHERE personal_schedule_id = ?")
@SQLRestriction("is_deleted = false")
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

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

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
        this.isDeleted = true;
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
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (title == null || title.trim().isEmpty()) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (title.trim().length() > 200) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (startAt == null || endAt == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (!endAt.isAfter(startAt)) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (status == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
        if (visibility == null) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }
    }
}
