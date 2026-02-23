package com.monsoon.seedflowplus.erd.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tbl_product")
public class ProductErd {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "crop")
    private String crop;

    @Column(name = "character", length = 10)
    private String character;

    @Column(name = "product_image_url")
    private String productImageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "status")
    private String status;

    @Column(name = "stock")
    private Integer stock;

    @Column(name = "money")
    private Long money;
}
