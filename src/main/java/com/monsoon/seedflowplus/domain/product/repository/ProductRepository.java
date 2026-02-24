package com.monsoon.seedflowplus.domain.product.repository;

import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByProductCategory(ProductCategory category); // 카테고리 필터링 검색

    @Query("SELECT p FROM Product p " +
            "JOIN ProductTag pt ON p = pt.product " +
            "JOIN pt.tag t " +
            "WHERE t.tagName = :environmentTag")
    List<Product> findProductsByEnvironmentTag(@Param("environmentTag") String environmentTag); // 재배환경 필터링 검색

    List<Product> findByProductNameContaining(String keyword);
}
