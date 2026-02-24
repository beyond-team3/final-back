package com.monsoon.seedflowplus.domain.product.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import com.monsoon.seedflowplus.erd.account.UserErd;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tbl_feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductFeedback extends BaseModifyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long feedbackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_key", nullable = false)
    private UserErd account;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Builder
    public ProductFeedback(Product product, UserErd account, String content) {
        this.product = product;
        this.account = account;
        this.content = content;
    }
}
