package com.monsoon.seedflowplus.erd;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tbl_notification")
public class NotificationErd {

    @Id
    @Column(name = "notice_id")
    private Long noticeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserErd user;

    @Column(name = "category")
    private String category;

    @Column(name = "event")
    private String event;

    @Column(name = "action")
    private String action;

    @Column(name = "link_url")
    private String linkUrl;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "target_type")
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;
}
