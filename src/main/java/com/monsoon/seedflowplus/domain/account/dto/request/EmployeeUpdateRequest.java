package com.monsoon.seedflowplus.domain.account.dto.request;

import jakarta.validation.constraints.Email;

public record EmployeeUpdateRequest(
        String employeeName,
        @Email(message = "이메일 형식이 올바르지 않습니다.") String employeeEmail,
        String employeePhone,
        String address) {
}
