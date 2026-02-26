package com.monsoon.seedflowplus.domain.account.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmployeeRegisterRequest(
        @NotBlank(message = "사원명은 필수 입력 값입니다.") String employeeName,
        @Email(message = "이메일 형식이 올바르지 않습니다.") @NotBlank(message = "이메일은 필수 입력 값입니다.") String employeeEmail,
        @NotBlank(message = "전화번호는 필수 입력 값입니다.") String employeePhone,
        @NotBlank(message = "주소는 필수 입력 값입니다.") String address) {
}
