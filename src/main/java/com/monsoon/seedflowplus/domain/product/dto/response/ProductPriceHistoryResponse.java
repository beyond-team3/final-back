package com.monsoon.seedflowplus.domain.product.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class ProductPriceHistoryResponse {

    private Long id;
    private Long productId;

    private BigDecimal oldPrice;
    private BigDecimal newPrice;

    private Long employeeId;
    private String employeeName;

    private LocalDateTime createdAt;
}
