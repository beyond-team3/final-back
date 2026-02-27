package com.monsoon.seedflowplus.domain.account.dto.request;

import com.monsoon.seedflowplus.domain.account.entity.Status;
import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(
        @NotNull(message = "사용자 ID는 필수입니다.") Long userId,

        @NotNull(message = "변경할 상태는 필수입니다.") Status status) {
}
