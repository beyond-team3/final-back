package com.monsoon.seedflowplus.domain.account.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PasswordChangeRequest(
        @NotBlank(message = "기존 비밀번호는 필수입니다.") String oldPassword,

        @NotBlank(message = "새 비밀번호는 필수입니다.") String newPassword) {
}
