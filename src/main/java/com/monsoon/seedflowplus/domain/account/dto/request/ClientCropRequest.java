package com.monsoon.seedflowplus.domain.account.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ClientCropRequest(
        @NotBlank(message = "품종 명칭은 필수입니다.") String cropName) {
}
