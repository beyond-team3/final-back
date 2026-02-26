package com.monsoon.seedflowplus.domain.account.dto.request;

public record EmployeeUpdateRequest(
        String employeeName,
        String employeeEmail,
        String employeePhone,
        String address) {
}
