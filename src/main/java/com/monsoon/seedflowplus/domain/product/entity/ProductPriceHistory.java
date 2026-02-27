package com.monsoon.seedflowplus.domain.product.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseCreateEntity;
import com.monsoon.seedflowplus.domain.account.entity.Employee;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tbl_product_price_history")
@AttributeOverride(name = "id", column = @Column(name = "product_price_history_id"))
public class ProductPriceHistory extends BaseCreateEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "old_price", nullable = false)
    private BigDecimal oldPrice;

    @Column(name = "new_price", nullable = false)
    private BigDecimal newPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee modifiedBy;

    @Builder
    public ProductPriceHistory(Product product, BigDecimal oldPrice, BigDecimal newPrice, Employee modifiedBy) {
        this.product = product;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
        this.modifiedBy = modifiedBy;
    }
}
