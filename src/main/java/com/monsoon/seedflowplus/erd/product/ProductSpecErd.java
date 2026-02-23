package com.monsoon.seedflowplus.erd;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
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
@Table(name = "tbl_product_spec")
public class ProductSpecErd {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "product_id")
    private ProductErd product;

    @Column(name = "cultivation_env_tags")
    private String cultivationEnvTags;

    @Column(name = "disease_resist_tags")
    private String diseaseResistTags;

    @Column(name = "growth_maturity_tags")
    private String growthMaturityTags;

    @Column(name = "fruit_quality_tags")
    private String fruitQualityTags;

    @Column(name = "cultivation_ease_tags")
    private String cultivationEaseTags;
}
