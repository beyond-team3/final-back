package com.monsoon.seedflowplus.domain.account;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@AttributeOverride(name = "id", column = @Column(name = "employee_id"))
@Table(name = "tbl_employee")
public class Employee extends BaseModifyEntity {

    @Column(name = "employee_code", nullable = false, unique = true)
    private String employeeCode;

    @Column(name = "employee_name", nullable = false)
    private String employeeName;

    @Column(name = "employee_email", nullable = false)
    private String employeeEmail;

    @Column(name = "employee_phone", nullable = false)
    private String employeePhone;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "account_id")
    private Long accountId;

}
