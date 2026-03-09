package com.monsoon.seedflowplus.domain.account.dto.request;

import com.monsoon.seedflowplus.domain.account.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCreateRequest(
        @NotBlank(message = "아이디는 필수 입력 값입니다.") String loginId,
        @NotBlank(message = "비밀번호는 필수 입력 값입니다.") String loginPw,
        @NotNull(message = "권한은 필수 입력 값입니다.") Role role,
        @NotNull(message = "연동할 대상(거래처/직원) ID는 필수 입력 값입니다.") Long targetId,
        Long employeeId // 거래처 담당 영업사원 ID (선택 사항)
) {
}
