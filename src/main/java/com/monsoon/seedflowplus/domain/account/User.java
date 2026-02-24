package com.monsoon.seedflowplus.domain.account;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
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
    @JoinColumn(name = "employee_code", referencedColumnName = "employeeCode")
    private Employee employee;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_code", referencedColumnName = "clientCode")
    private Client client;

    @Column(name = "account_id")
    private Long accountId;

}
