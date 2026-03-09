package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.domain.account.entity.Employee;

public record EmployeeSimpleResponse(
        Long employeeId,
        String employeeCode,
        String employeeName) {
    public static EmployeeSimpleResponse from(Employee employee) {
        return new EmployeeSimpleResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getEmployeeName());
    }
}
