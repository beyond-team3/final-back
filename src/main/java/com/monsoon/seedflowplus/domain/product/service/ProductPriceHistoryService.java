package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.product.dto.response.ProductPriceHistoryResponse;
import com.monsoon.seedflowplus.domain.product.entity.ProductPriceHistory;
import com.monsoon.seedflowplus.domain.product.repository.ProductPriceHistoryRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductPriceHistoryService {

    private final ProductPriceHistoryRepository productPriceHistoryRepository;
    private final ProductRepository productRepository;

    public List<ProductPriceHistoryResponse> getPriceHistories(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new CoreException(ErrorType.PRODUCT_NOT_FOUND);
        }

        List<ProductPriceHistory> historyList = productPriceHistoryRepository.findByProductIdWithEmployee(productId);

        return historyList.stream().map(history -> ProductPriceHistoryResponse.builder()
                .id(history.getId())
                .productId(history.getProduct().getId())
                .oldPrice(history.getOldPrice())
                .newPrice(history.getNewPrice())
                .employeeId(history.getModifiedBy().getId())
                .employeeName(history.getModifiedBy().getEmployeeName())
                .createdAt(history.getCreatedAt())
                .build())
                .collect(Collectors.toList());
    }
}
