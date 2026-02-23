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
@Table(name = "tbl_employee")
public class Employee extends BaseModifyEntity {

    @Column(nullable = false, unique = true)
    private String employeeCode;

    @Column(nullable = false)
    private String employeeName;

    @Column(nullable = false)
    private String employeeEmail;

    @Column(nullable = false)
    private String employeePhone;

    @Column(nullable = false)
    private String address;

    @Column(name = "account_key")
    private Long accountId;

}
