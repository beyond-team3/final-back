package com.monsoon.seedflowplus.domain.product.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import com.monsoon.seedflowplus.domain.account.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tbl_product_compare")
@AttributeOverride(name = "id", column = @Column(name = "compare_id"))
public class ProductCompare extends BaseModifyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private User account;

    private String title;

    @OneToMany(mappedBy = "productCompare", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductCompareItem> items = new ArrayList<>();

    @Builder
    public ProductCompare(User account, String title) {
        this.account = account;
        this.title = title;
    }

    public void addItems(List<ProductCompareItem> newItems) {
        this.items.addAll(newItems);
        newItems.forEach(item -> item.setProductCompare(this));
    }
}
