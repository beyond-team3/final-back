package com.monsoon.seedflowplus.domain.sales.rfq.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseEntity;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AttributeOverride(name = "id", column = @Column(name = "rfq_detail_id"))
@Table(name = "tbl_request_quotation_detail")
public class QuotationRequestDetail extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_request_id")
    private QuotationRequestHeader quotationRequest;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product; // 제품ID

    @Column(name = "product_category")
    private String productCategory; // 품종(예: 배추, 토마토...)
    @Column(name = "product_name")
    private String productName; // 제품명
    private Integer quantity; // 수량(구매 수량)

}
