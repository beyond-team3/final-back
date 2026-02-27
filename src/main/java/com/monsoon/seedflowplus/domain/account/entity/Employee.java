package com.monsoon.seedflowplus.domain.account.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import jakarta.persistence.*;
import lombok.*;

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

    @Builder
    public Employee(String employeeCode, String employeeName, String employeeEmail, String employeePhone,
                    String address) {
        this.employeeCode = employeeCode;
        this.employeeName = employeeName;
        this.employeeEmail = employeeEmail;
        this.employeePhone = employeePhone;
        this.address = address;
    }

    public void updateEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public void updateEmployeeInfo(String employeeName, String employeeEmail, String employeePhone, String address) {
        if (employeeName != null && !employeeName.isBlank())
            this.employeeName = employeeName.trim();
        if (employeeEmail != null && !employeeEmail.isBlank())
            this.employeeEmail = employeeEmail.trim();
        if (employeePhone != null && !employeePhone.isBlank())
            this.employeePhone = employeePhone.trim();
        if (address != null && !address.isBlank())
            this.address = address.trim();
    }

}