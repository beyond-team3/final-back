package com.monsoon.seedflowplus.erd.product;

import com.monsoon.seedflowplus.core.common.entity.BaseCreateEntity;
import com.monsoon.seedflowplus.erd.account.UserErd;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "tbl_bookmark",
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
public class BookmarkErd extends BaseCreateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookmark_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_key", nullable = false)
    private UserErd account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductErd product;

    @Builder
    public BookmarkErd(UserErd account, ProductErd product) {
        this.account = account;
        this.product = product;
    }
}