package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.ProductBookmark;
import com.monsoon.seedflowplus.domain.product.repository.ProductBookmarkRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductBookmarkService {

    private final ProductBookmarkRepository productBookmarkRepository;
//    private final UserRepository userRepository;  // 유저 레포지토리 없어서 주석처리
    private final ProductRepository productRepository;

    // 즐겨찾기 토글
    public void toggleBookmark(Long userId, Long productId) {
        Optional<ProductBookmark> existingBookmark =
                productBookmarkRepository.findByAccount_IdAndProduct_Id(userId, productId);

        if (existingBookmark.isPresent()) {
            productBookmarkRepository.delete(existingBookmark.get());
        } else {
//            User account = userRepository.findById(userId)
//                    .orElseThrow(() -> new CoreException(ErrorType.USER_NOT_FOUND));  // 유저 레포지토리 없어서 주석처리
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new CoreException(ErrorType.PRODUCT_NOT_FOUND));

            ProductBookmark newBookmark = ProductBookmark.builder()
//                    .account(account)  // 유저 레포지토리 없어서 주석처리
                    .product(product)
                    .build();

            productBookmarkRepository.save(newBookmark);
        }
    }
}
