package com.monsoon.seedflowplus.domain.product.repository;

import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {

    List<Product> findByProductCategory(ProductCategory category); // 카테고리 필터링 검색

    @Query("SELECT p FROM Product p " +
            "JOIN ProductTag pt ON p = pt.product " +
            "JOIN pt.tag t " +
            "WHERE t.tagName = :environmentTag")
    List<Product> findProductsByEnvironmentTag(@Param("environmentTag") String environmentTag); // 재배환경 필터링 검색

    List<Product> findByProductNameContaining(String keyword);

    Optional<Product> findTopByProductCategoryOrderByIdDesc(ProductCategory productCategory);

    // 유사 상품 추천용: 자신을 제외한 같은 카테고리 상품 최신순 5개 조회
    List<Product> findAllByProductCategoryAndIdNot(ProductCategory category, Long id);
}
