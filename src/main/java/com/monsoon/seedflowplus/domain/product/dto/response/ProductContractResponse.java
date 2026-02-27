package com.monsoon.seedflowplus.domain.product.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ProductContractResponse {

    private Long productId;
    private String productCategory; // 품종
    private String productName; // 상품명
    private String unit; // 단위
    private BigDecimal price; // 표준단가

}
