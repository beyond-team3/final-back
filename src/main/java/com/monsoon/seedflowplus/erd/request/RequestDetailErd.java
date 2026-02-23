package com.monsoon.seedflowplus.erd;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
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
@Table(name = "tbl_request_detail")
public class RequestDetailErd {

    @Id
    @Column(name = "request_detail_id")
    private Long requestDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private RequestHeaderErd request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id2", nullable = false)
    private ProductErd product;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "request_quantity", nullable = false)
    private Integer requestQuantity;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Lob
    @Column(name = "Field")
    private String field;
}
