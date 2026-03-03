package com.monsoon.seedflowplus.domain.product.repository;

import com.monsoon.seedflowplus.domain.product.entity.ProductCompareItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductCompareItemRepository extends JpaRepository<ProductCompareItem, Long> {

    @Modifying
    @Query("DELETE FROM ProductCompareItem pci WHERE pci.product.id = :productId")
    void deleteAllByProductId(@Param("productId") Long productId);
}
