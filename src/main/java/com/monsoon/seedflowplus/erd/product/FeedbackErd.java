package com.monsoon.seedflowplus.erd.product;

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
public class FeedbackErd extends BaseModifyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductErd product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_key", nullable = false)
    private UserErd account;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Builder
    public FeedbackErd(ProductErd product, UserErd account, String content) {
        this.product = product;
        this.account = account;
        this.content = content;
    }
}
