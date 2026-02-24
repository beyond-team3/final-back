package com.monsoon.seedflowplus.domain.product.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import com.monsoon.seedflowplus.domain.product.dto.request.ProductUpdateParam;
import jakarta.persistence.*;


import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tbl_product")
@AttributeOverride(name = "id", column = @Column(name = "product_id"))
public class Product extends BaseModifyEntity {

    @Column(nullable = false, length = 50)
    private String productCode;

    @Column(nullable = false, length = 100)
    private String productName;

    @Enumerated(EnumType.STRING)
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

    // JSON 타입으로 태그 저장
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, List<String>> tags = new HashMap<>();

    @Builder
    public Product(String productCode, String productName, ProductCategory productCategory,
                   String productDescription, String productImageUrl, Integer amount,
                   String unit, BigDecimal price, ProductStatus status,
                   Map<String, List<String>> tags) {
        this.productCode = productCode;
        this.productName = productName;
        this.productCategory = productCategory;
        this.productDescription = productDescription;
        this.productImageUrl = productImageUrl;
        this.amount = amount;
        this.unit = unit;
        this.price = price;
        this.status = status;
        this.tags = tags;
    }

    public void updateProduct(ProductUpdateParam param, Map<String, List<String>> tags) {
        this.productName = param.productName();
        this.productCategory = ProductCategory.valueOf(param.productCategory()); // String -> Enum
        this.productDescription = param.productDescription();
        this.productImageUrl = param.productImageUrl();
        this.amount = param.amount();
        this.unit = param.unit();
        this.price = param.price();
        this.status = ProductStatus.valueOf(param.status());                     // String -> Enum
        this.tags = tags;
    }
}
