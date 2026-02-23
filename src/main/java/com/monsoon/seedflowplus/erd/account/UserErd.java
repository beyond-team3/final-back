package com.monsoon.seedflowplus.erd.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "tbl_user")
public class UserErd {

    @Id
    @Column(name = "account_key")
    private Long accountKey;

    @Column(name = "login_id", nullable = false)
    private String loginId;

    @Column(name = "login_pw", nullable = false)
    private String loginPw;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "account_type", nullable = false)
    private String accountType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
