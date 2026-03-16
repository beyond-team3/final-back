package com.monsoon.seedflowplus.domain.product.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FeedbackRequest {

    @NotBlank(message = "피드백 내용은 필수입니다.")
    private String content;

    // 답글(대댓글)인 경우 부모 피드백 ID (null이면 최상위 피드백)
    private Long parentId;

}
