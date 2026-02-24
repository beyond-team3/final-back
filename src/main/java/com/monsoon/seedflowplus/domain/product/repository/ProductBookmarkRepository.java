package com.monsoon.seedflowplus.domain.product.repository;

import com.monsoon.seedflowplus.domain.product.entity.ProductBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductBookmarkRepository extends JpaRepository<ProductBookmark, Long> {

    List<ProductBookmark> findByAccount_Id(Long accountId);

    @Query("SELECT b FROM ProductBookmark b JOIN FETCH b.product WHERE b.account.id = :accountId")
    List<ProductBookmark> findMyBookmarksWithProduct(@Param("accountId") Long accountId);
}
