package com.monsoon.seedflowplus.domain.account.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@AttributeOverride(name = "id", column = @Column(name = "user_id"))
@Table(name = "tbl_user")
public class User extends BaseModifyEntity {

    @Column(name = "login_id", nullable = false)
    private String loginId;

    @Column(name = "login_pw", nullable = false)
    private String loginPw;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Builder
    public User(String loginId, String loginPw, Status status, Role role, Employee employee, Client client) {
        this.loginId = loginId;
        this.loginPw = loginPw;
        this.status = status;
        this.role = role;
        this.employee = employee;
        this.client = client;
    }

    public void updateLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public void updateStatus(Status status) {
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    public void updatePassword(String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("newPassword must not be blank");
        }
        this.loginPw = newPassword;
    }

}