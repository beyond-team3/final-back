package com.monsoon.seedflowplus.domain.schedule.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import com.monsoon.seedflowplus.domain.account.entity.User;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
                @Index(name = "idx_owner_start_at", columnList = "owner_id, start_at"),
                @Index(name = "idx_owner_end_at", columnList = "owner_id, end_at")
        }
)
public class PersSked extends BaseModifyEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Builder
    public PersSked(String title, String description, User owner, LocalDateTime startAt, LocalDateTime endAt) {
        validate(title, owner, startAt, endAt);
        this.title = title.trim();
        this.description = description;
        this.owner = owner;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public void update(String title, String description, LocalDateTime startAt, LocalDateTime endAt) {
        validate(title, this.owner, startAt, endAt);
        this.title = title.trim();
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    private void validate(String title, User owner, LocalDateTime startAt, LocalDateTime endAt) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (owner == null) {
            throw new IllegalArgumentException("owner must not be null");
        }
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException("startAt/endAt must not be null");
        }
        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }
    }
}
