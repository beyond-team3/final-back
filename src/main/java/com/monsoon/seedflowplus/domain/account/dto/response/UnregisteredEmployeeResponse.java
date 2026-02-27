package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.domain.account.entity.Employee;

public record UnregisteredEmployeeResponse(
        Long employeeId,
        String employeeCode,
        String employeeName) {
    public static UnregisteredEmployeeResponse from(Employee employee) {
        return new UnregisteredEmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getEmployeeName());
    }
}
