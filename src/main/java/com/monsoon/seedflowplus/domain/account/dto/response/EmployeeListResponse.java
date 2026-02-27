package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;

public record EmployeeListResponse(
        String employeeCode,
        String employeeName,
        String employeeEmail,
        Status status) {
    public static EmployeeListResponse from(User user) {
        if (user.getEmployee() == null) {
            return null;
        }
        return new EmployeeListResponse(
                user.getEmployee().getEmployeeCode(),
                user.getEmployee().getEmployeeName(),
                user.getEmployee().getEmployeeEmail(),
                user.getStatus());
    }
}
