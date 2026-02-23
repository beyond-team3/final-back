package com.monsoon.seedflowplus.erd.product;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import jakarta.persistence.*;


import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tbl_product") // 명세서의 테이블명 반영
public class ProductErd extends BaseModifyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId; // 상품pk

    @Column(nullable = false, length = 50)
    private String productCode; // 상품코드

    @Column(nullable = false, length = 100)
    private String productName; // 상품명

    @Column(nullable = false, length = 50)
    private String productCategory; // 상품 카테고리 (명세서 기준)

    @Column(columnDefinition = "TEXT")
    private String productDescription; // 상품 설명

    private String productImageUrl; // 상품이미지url

    @Column(nullable = false)
    private Integer amount; // 수량

    @Column(nullable = false, length = 20)
    private String unit; // 단위 (예: kg, 박스 등)

    @Column(nullable = false)
    private Integer price; // 단가

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProductStatus status; // 상태 (판매중, 중단 등)

    @Builder
    public ProductErd(String productCode, String productName, String productCategory,
                   String productDescription, String productImageUrl, Integer amount,
                   String unit, Integer price, ProductStatus status) {
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
