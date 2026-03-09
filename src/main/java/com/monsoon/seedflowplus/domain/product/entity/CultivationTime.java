package com.monsoon.seedflowplus.domain.product.entity;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@AttributeOverride(name = "id", column = @Column(name = "cultivation_time_id"))
@Table(name = "tbl_cultivation_time")
public class CultivationTime extends BaseModifyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id", nullable = false)
    private Product product;

    @Column(length = 50)
    private String croppingSystem;

    @Column(length = 50)
    private String region;

    private Integer sowingStart;

    private Integer sowingEnd;

    private Integer plantingStart;

    private Integer plantingEnd;

    private Integer harvestingStart;

    private Integer harvestingEnd;

    @Builder
    public CultivationTime(Product product, String croppingSystem, String region, Integer sowingStart,
            Integer sowingEnd, Integer plantingStart,
            Integer plantingEnd, Integer harvestingStart, Integer harvestingEnd) {
        this.product = product;
        this.croppingSystem = croppingSystem;
        this.region = region;
        this.sowingStart = sowingStart;
        this.sowingEnd = sowingEnd;
        this.plantingStart = plantingStart;
        this.plantingEnd = plantingEnd;
        this.harvestingStart = harvestingStart;
        this.harvestingEnd = harvestingEnd;
    }
}
