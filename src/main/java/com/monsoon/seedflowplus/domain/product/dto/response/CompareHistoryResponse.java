package com.monsoon.seedflowplus.domain.product.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class CompareHistoryResponse {
    private Long compareId;
    private String title;
    private LocalDateTime createdAt;
    private List<ProductResponse> products;
}
