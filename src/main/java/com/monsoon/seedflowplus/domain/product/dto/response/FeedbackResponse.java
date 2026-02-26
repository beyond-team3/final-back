package com.monsoon.seedflowplus.domain.product.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FeedbackResponse {

    private Long id;
    private Long productId;

    private Long employeeId;
    private String employeeName; // Employee의 이름 (존재할 경우)

    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
