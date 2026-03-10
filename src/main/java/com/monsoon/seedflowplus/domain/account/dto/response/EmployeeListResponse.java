package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;

public record EmployeeListResponse(
        Long employeeId,
        String employeeCode,
        String employeeName,
        String employeeEmail,
        Status status) {
    public static EmployeeListResponse from(User user) {
        if (user.getEmployee() == null) {
            return null;
        }
        return new EmployeeListResponse(
                user.getEmployee().getId(),
                user.getEmployee().getEmployeeCode(),
                user.getEmployee().getEmployeeName(),
                user.getEmployee().getEmployeeEmail(),
                user.getStatus());
    }

    public static EmployeeListResponse from(Employee employee, Status status) {
        return new EmployeeListResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getEmployeeName(),
                employee.getEmployeeEmail(),
                status);
    }
}

