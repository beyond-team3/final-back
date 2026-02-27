package com.monsoon.seedflowplus.domain.product.repository;

import com.monsoon.seedflowplus.domain.product.dto.request.ProductSearchCondition;
import com.monsoon.seedflowplus.domain.product.entity.Product;

import java.util.List;

public interface ProductRepositoryCustom {
    List<Product> searchByCondition(ProductSearchCondition condition);
}
