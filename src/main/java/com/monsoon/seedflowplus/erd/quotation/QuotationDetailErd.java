package com.monsoon.seedflowplus.erd.quotation;

import com.monsoon.seedflowplus.erd.product.ProductErd;
import com.monsoon.seedflowplus.erd.request.RequestDetailErd;
import com.monsoon.seedflowplus.erd.request.RequestHeaderErd;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tbl_quotation_detail")
public class QuotationDetailErd {

    @Id
    @Column(name = "quotation_detail_id")
    private Long quotationDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    private QuotationHeaderErd quotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_detail_id", nullable = false)
    private RequestDetailErd requestDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private RequestHeaderErd request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductErd product;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    @Column(name = "unit", nullable = false)
    private String unit;

    @Column(name = "fee", nullable = false)
    private Integer fee;
}
