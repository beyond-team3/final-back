package com.monsoon.seedflowplus.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tbl_cultivation_time")
public class CultivationTime {

    @Id
    @Column(name = "cultivation_time_id")
    private Long cultivationTimeId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sowing_start")
    private Integer sowingStart;

    @Column(name = "sowing_end")
    private Integer sowingEnd;

    @Column(name = "planting_start")
    private Integer plantingStart;

    @Column(name = "planting_end")
    private Integer plantingEnd;

    @Column(name = "harvesting_start")
    private Integer harvestingStart;

    @Column(name = "harvesting_end")
    private Integer harvestingEnd;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

}
