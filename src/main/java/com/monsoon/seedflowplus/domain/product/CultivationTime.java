package com.monsoon.seedflowplus.domain.product;

import com.monsoon.seedflowplus.core.common.entity.BaseModifyEntity;
import jakarta.persistence.*;
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
public class CultivationTime extends BaseModifyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cultivationTimeId;

    @Column(nullable = false)
    private Long productId;

    private Integer sowingStart;

    private Integer sowingEnd;

    private Integer plantingStart;

    private Integer plantingEnd;

    private Integer harvestingStart;

    private Integer harvestingEnd;
}
