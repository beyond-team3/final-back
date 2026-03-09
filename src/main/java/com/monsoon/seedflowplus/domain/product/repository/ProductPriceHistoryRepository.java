package com.monsoon.seedflowplus.domain.product.repository;

import com.monsoon.seedflowplus.domain.product.entity.ProductPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductPriceHistoryRepository extends JpaRepository<ProductPriceHistory, Long> {

    @Query("SELECT p FROM ProductPriceHistory p JOIN FETCH p.modifiedBy WHERE p.product.id = :productId ORDER BY p.createdAt DESC")
    List<ProductPriceHistory> findByProductIdWithEmployee(@Param("productId") Long productId);
}
