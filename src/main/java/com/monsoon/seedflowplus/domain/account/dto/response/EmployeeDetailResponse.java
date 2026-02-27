package com.monsoon.seedflowplus.domain.account.dto.response;

import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;

public record EmployeeDetailResponse(
        String employeeName,
        String employeeCode,
        String employeeEmail,
        String employeePhone,
        String address,
        Status status) {
    public static EmployeeDetailResponse from(User user) {
        if (user.getEmployee() == null) {
            return null;
        }
        return new EmployeeDetailResponse(
                user.getEmployee().getEmployeeName(),
                user.getEmployee().getEmployeeCode(),
                user.getEmployee().getEmployeeEmail(),
                user.getEmployee().getEmployeePhone(),
                user.getEmployee().getAddress(),
                user.getStatus());
    }
}
