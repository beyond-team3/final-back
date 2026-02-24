package com.monsoon.seedflowplus.domain.product.repository;

import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.ProductFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductFeedbackRepository extends JpaRepository<ProductFeedback, Long> {

    List<ProductFeedback> findByEmployee_Id(Long employeeId);

    @Query("SELECT f FROM ProductFeedback f JOIN FETCH f.employee WHERE f.product.id = :productId")
    List<ProductFeedback> findByProductIdWithEmployee(@Param("productId") Long productId);
}
