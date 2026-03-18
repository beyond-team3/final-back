package com.monsoon.seedflowplus.domain.product.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.ProductBookmark;
import com.monsoon.seedflowplus.domain.product.entity.ProductCategory;
import com.monsoon.seedflowplus.domain.product.entity.ProductStatus;
import com.monsoon.seedflowplus.domain.product.repository.ProductBookmarkRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ProductBookmarkServiceTest {

    @Mock
    private ProductBookmarkRepository productBookmarkRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductBookmarkService productBookmarkService;

    @Test
    @DisplayName("즐겨찾기가 없을 때 토글하면 새 북마크가 저장된다")
    void toggleBookmark_Add() {
        // given
        Long userId = 1L;
        Long productId = 10L;

        User user = createUser(userId);
        Product product = createProduct(productId);

        when(productBookmarkRepository.findByAccount_IdAndProduct_Id(userId, productId))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // when
        productBookmarkService.toggleBookmark(userId, productId);

        // then
        verify(productBookmarkRepository, times(1)).save(any(ProductBookmark.class));
        verify(productBookmarkRepository, never()).delete(any());
    }

    @Test
    @DisplayName("즐겨찾기가 이미 있을 때 토글하면 북마크가 삭제된다")
    void toggleBookmark_Remove() {
        // given
        Long userId = 1L;
        Long productId = 10L;

        ProductBookmark existingBookmark = mock(ProductBookmark.class);
        when(productBookmarkRepository.findByAccount_IdAndProduct_Id(userId, productId))
                .thenReturn(Optional.of(existingBookmark));

        // when
        productBookmarkService.toggleBookmark(userId, productId);

        // then
        verify(productBookmarkRepository, times(1)).delete(existingBookmark);
        verify(productBookmarkRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 즐겨찾기 추가 시 USER_NOT_FOUND 예외가 발생한다")
    void toggleBookmark_UserNotFound() {
        // given
        Long userId = 999L;
        Long productId = 10L;

        when(productBookmarkRepository.findByAccount_IdAndProduct_Id(userId, productId))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productBookmarkService.toggleBookmark(userId, productId))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 즐겨찾기 추가 시 PRODUCT_NOT_FOUND 예외가 발생한다")
    void toggleBookmark_ProductNotFound() {
        // given
        Long userId = 1L;
        Long productId = 999L;

        User user = createUser(userId);
        when(productBookmarkRepository.findByAccount_IdAndProduct_Id(userId, productId))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productBookmarkService.toggleBookmark(userId, productId))
                .isInstanceOf(CoreException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.PRODUCT_NOT_FOUND);
    }

    // ─── 헬퍼 ──────────────────────────────────────────────────────────────────

    private User createUser(Long id) {
        User user = User.builder()
                .loginId("user-" + id)
                .loginPw("pw")
                .status(Status.ACTIVATE)
                .role(Role.SALES_REP)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Product createProduct(Long id) {
        Product product = Product.builder()
                .productCode("WM-001")
                .productName("수박 씨앗")
                .productCategory(ProductCategory.WATERMELON)
                .amount(100)
                .unit("BOX")
                .price(new BigDecimal("50000"))
                .status(ProductStatus.SALE)
                .build();
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }
}
