package com.monsoon.seedflowplus.domain.account.dto.request;

import com.monsoon.seedflowplus.domain.account.entity.ClientType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ClientRegisterRequest(
        String clientCode,
        @NotBlank(message = "법인명은 필수 입력 값입니다.") String clientName,
        @NotBlank(message = "사업자등록번호는 필수 입력 값입니다.") String clientBrn,
        @NotBlank(message = "대표자명은 필수 입력 값입니다.") String ceoName,
        @NotBlank(message = "회사 전화번호는 필수 입력 값입니다.") String companyPhone,
        @NotBlank(message = "주소는 필수 입력 값입니다.") String address,
        @NotNull(message = "거래처 구분은 필수 입력 값입니다.") ClientType clientType,
        @NotBlank(message = "담당자명은 필수 입력 값입니다.") String managerName,
        @NotBlank(message = "담당자 전화번호는 필수 입력 값입니다.") String managerPhone,
        @Email(message = "이메일 형식이 올바르지 않습니다.") @NotBlank(message = "담당자 이메일은 필수 입력 값입니다.") String managerEmail,
        @NotNull(message = "여신 금액은 필수 입력 값입니다.") @PositiveOrZero(message = "여신 금액은 0 이상이어야 합니다.") BigDecimal totalCredit
) {
}
