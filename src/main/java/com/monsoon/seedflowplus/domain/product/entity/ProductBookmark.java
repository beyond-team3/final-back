package com.monsoon.seedflowplus.domain.product.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseCreateEntity;
import com.monsoon.seedflowplus.domain.account.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "tbl_product_bookmark",
        // 한 명의 유저가 동일한 상품을 여러 번 즐겨찾기 할 수 없도록 복합 유니크 제약조건 설정
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_bookmark_account_product",
                        columnNames = {"account_key", "product_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "id", column = @Column(name = "product_bookmark_id"))
public class ProductBookmark extends BaseCreateEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_key", nullable = false)
    private User account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Builder
    public ProductBookmark(User account, Product product) {
        this.account = account;
        this.product = product;
    }
}