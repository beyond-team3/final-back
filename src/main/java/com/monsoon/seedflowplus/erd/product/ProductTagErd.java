package com.monsoon.seedflowplus.erd.product;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tbl_product_tag")
public class ProductTagErd extends BaseCreateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productTagId; // 상품 태그pk

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductErd product; // 상품pk (연관관계)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private TagErd tag; // 태그pk (연관관계)

    @Builder
    public ProductTagErd(ProductErd product, TagErd tag) {
        this.product = product;
        this.tag = tag;
    }
}