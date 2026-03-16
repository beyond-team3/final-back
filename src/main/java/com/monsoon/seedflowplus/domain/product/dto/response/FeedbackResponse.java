package com.monsoon.seedflowplus.domain.product.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FeedbackResponse {

    private Long id;
    private Long productId;
    private Long parentId;       // 답글인 경우 부모 피드백 ID, 최상위이면 null

    private Long employeeId;
    private String employeeName; // Employee의 이름 (존재할 경우)
    private String sender;       // 프론트엔드 호환용 표시명 (= employeeName)

    private boolean isMine;      // 요청한 사용자가 작성한 피드백인지 여부

    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
