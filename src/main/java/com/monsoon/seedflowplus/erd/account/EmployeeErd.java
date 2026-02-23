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
@Table(name = "tbl_employee")
public class EmployeeErd {

    @Id
    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "employee_name", nullable = false)
    private String employeeName;

    @Column(name = "employee_email", nullable = false)
    private String employeeEmail;

    @Column(name = "emp_phone", nullable = false)
    private String empPhone;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_key", nullable = false)
    private UserErd account;

    @Column(name = "address", nullable = false)
    private String address;
}
