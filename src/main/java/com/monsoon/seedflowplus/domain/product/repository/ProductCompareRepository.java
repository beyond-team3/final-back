package com.monsoon.seedflowplus.domain.product.repository;

import com.monsoon.seedflowplus.domain.product.entity.ProductCompare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductCompareRepository extends JpaRepository<ProductCompare, Long> {

    @Query("SELECT DISTINCT pc FROM ProductCompare pc JOIN FETCH pc.items i JOIN FETCH i.product WHERE pc.account.id = :accountId ORDER BY pc.createdAt DESC")
    List<ProductCompare> findAllByAccountIdWithItems(@Param("accountId") Long accountId);
}
