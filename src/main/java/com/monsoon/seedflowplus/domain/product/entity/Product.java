package com.monsoon.seedflowplus.domain.product.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import jakarta.persistence.*;


import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tbl_product")
public class Product extends BaseModifyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Column(nullable = false, length = 50)
    private String productCode;

    @Column(nullable = false, length = 100)
    private String productName;

    @Column(nullable = false, length = 50)
    private ProductCategory productCategory;

    @Column(columnDefinition = "TEXT")
    private String productDescription;

    private String productImageUrl;

    @Column(nullable = false)
    private Integer amount; // 수량

    @Column(nullable = false, length = 20)
    private String unit; // 단위 (예: kg, 박스 등)

    @Column(nullable = false)
    private BigDecimal price; // 단가

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProductStatus status; // 상태 (판매중, 중단 등)

    @Builder
    public Product(String productCode, String productName, ProductCategory productCategory,
                   String productDescription, String productImageUrl, Integer amount,
                   String unit, BigDecimal price, ProductStatus status) {
        this.productCode = productCode;
        this.productName = productName;
        this.productCategory = productCategory;
        this.productDescription = productDescription;
        this.productImageUrl = productImageUrl;
        this.amount = amount;
        this.unit = unit;
        this.price = price;
        this.status = status;
    }
}
