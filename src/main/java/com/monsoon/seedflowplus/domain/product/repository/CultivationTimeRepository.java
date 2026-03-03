package com.monsoon.seedflowplus.domain.product.repository;

import com.monsoon.seedflowplus.domain.product.entity.CultivationTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CultivationTimeRepository extends JpaRepository<CultivationTime, Long> {

    Optional<CultivationTime> findByProductId(Long productId);

    java.util.List<CultivationTime> findAllByProductIdIn(java.util.List<Long> productIds);
}
