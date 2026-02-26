package com.monsoon.seedflowplus.domain.account.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record EmployeeUpdateRequest(
        String employeeName,
        @Email(message = "이메일 형식이 올바르지 않습니다.") String employeeEmail,
        @Pattern(regexp = "^$|^[0-9-]{9,20}$", message = "전화번호 형식이 올바르지 않습니다.") String employeePhone,
        String address) {
}
