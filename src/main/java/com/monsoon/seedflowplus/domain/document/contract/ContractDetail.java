package com.monsoon.seedflowplus.domain.document.contract;

import com.monsoon.seedflowplus.core.common.entity.BaseEntity;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "id", column = @Column(name = "cnt_detail_id"))
@Table(name = "tbl_contract_detail")
public class ContractDetail extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cnt_id", nullable = false)
    private ContractHeader contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product; // 제품 연결

    @Column(name = "product_category")
    private String productCategory; // 품종 (이력 보관용) 예) 토마토, 배추 ...

    @Column(name = "product_name", nullable = false)
    private String productName; // 이력 보관용 제품명

    @Column(name = "total_quantity")
    private Integer totalQuantity; // 주문 가능한 수량

    @Column(name = "unit")
    private String unit; // 단위 >> 상품에 명시되어있는 것

    @Column(name = "unit_price")
    private BigDecimal unitPrice; // 합의한 단가

    @Column(name = "amount")
    private BigDecimal amount; // 소계(단가 × 수량)
}
