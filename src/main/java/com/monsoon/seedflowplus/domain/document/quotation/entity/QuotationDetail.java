package com.monsoon.seedflowplus.domain.document.quotation.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseEntity;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "id", column = @Column(name = "quo_detail_id"))
@Table(name = "tbl_quotation_detail")
public class QuotationDetail extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quo_id", nullable = false)
    private QuotationHeader quotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product; // 제품 연결

    @Column(name = "product_category")
    private String productCategory; // 품종 (이력 보관용) 예) 토마토, 배추 ...

    @Column(name = "product_name", nullable = false)
    private String productName; // 제품명 (이력 보관용)

    @Column(name = "quantity")
    private Integer quantity; // 수량

    @Column(name = "unit")
    private String unit; // 단위

    @Column(name = "unit_price")
    private BigDecimal unitPrice; // 단가 (견적 당시 가격) >> 제품에서 우선 들고 온 가격에 수정을 더한 가격

    @Column(name = "amount")
    private BigDecimal amount; // 소계
}
