package com.monsoon.seedflowplus.domain.product.repository;

import com.monsoon.seedflowplus.domain.product.entity.ProductBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductBookmarkRepository extends JpaRepository<ProductBookmark, Long> {

    List<ProductBookmark> findByAccount_Id(Long accountId);

    @Query("SELECT b FROM ProductBookmark b JOIN FETCH b.product WHERE b.account.id = :accountId")
    List<ProductBookmark> findMyBookmarksWithProduct(@Param("accountId") Long accountId);

    // 즐겨찾기 중복 방지를 위한 확인
    Optional<ProductBookmark> findByAccount_IdAndProduct_Id(Long accountId, Long productId);

    // 특정 상품 삭제시 즐겨찾기 함께 삭제
    @Modifying
    @Query("DELETE FROM ProductBookmark pb WHERE pb.product.id = :productId")
    void deleteAllByProductId(@Param("productId") Long productId);
}
