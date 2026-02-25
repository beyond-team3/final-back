package com.monsoon.seedflowplus.domain.product.repository;

import com.monsoon.seedflowplus.domain.product.entity.ProductTag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTagRepository extends JpaRepository<ProductTag, Long> {
    // 특정 상품에 연결된 모든 태그 매핑을 한 번에 삭제
    void deleteByProduct_Id(Long productId);
}