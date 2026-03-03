package com.monsoon.seedflowplus.domain.product.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tbl_product_compare_item")
@AttributeOverride(name = "id", column = @Column(name = "compare_item_id"))
public class ProductCompareItem extends BaseModifyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compare_id")
    private ProductCompare productCompare;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Builder
    public ProductCompareItem(Product product) {
        this.product = product;
    }

    public void setProductCompare(ProductCompare productCompare) {
        this.productCompare = productCompare;
    }
}
