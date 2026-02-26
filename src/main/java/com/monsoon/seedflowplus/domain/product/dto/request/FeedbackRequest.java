package com.monsoon.seedflowplus.domain.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FeedbackRequest {

    @NotBlank(message = "피드백 내용은 필수입니다.")
    private String content;

}
