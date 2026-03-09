package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.domain.account.entity.Employee;

public record AssignedEmployeeResponse(
        String employeeName,
        String employeePhone,
        String employeeEmail) {
    public static AssignedEmployeeResponse from(Employee employee) {
        if (employee == null) {
            return none();
        }
        return new AssignedEmployeeResponse(
                employee.getEmployeeName(),
                employee.getEmployeePhone(),
                employee.getEmployeeEmail());
    }

    public static AssignedEmployeeResponse none() {
        return new AssignedEmployeeResponse("미배정", "-", "-");
    }
}
