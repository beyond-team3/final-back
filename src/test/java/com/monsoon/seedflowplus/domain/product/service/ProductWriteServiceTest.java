package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.product.dto.request.ProductRequest;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.ProductCategory;
import com.monsoon.seedflowplus.domain.product.entity.ProductStatus;
import com.monsoon.seedflowplus.domain.product.repository.ProductBookmarkRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductPriceHistoryRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductTagRepository;
import com.monsoon.seedflowplus.domain.product.repository.CultivationTimeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductWriteServiceTest {

        @Mock
        private ProductRepository productRepository;

        @Mock
        @SuppressWarnings("unused")
        private ProductBookmarkRepository productBookmarkRepository;

        @Mock
        @SuppressWarnings("unused")
        private TagService tagService;

        @Mock
        @SuppressWarnings("unused")
        private ProductTagRepository productTagRepository;

        @Mock
        private ProductPriceHistoryRepository productPriceHistoryRepository;

        @Mock
        @SuppressWarnings("unused")
        private CultivationTimeRepository cultivationTimeRepository;

        @Mock
        private UserRepository userRepository;

        @InjectMocks
        private ProductWriteService productWriteService;

        @Test
        @DisplayName("신규 상품을 등록하면 저장소가 호출되어야 한다")
        void testCreateProduct() {
                // given
                ProductRequest request = mock(ProductRequest.class);
                when(request.getProductCategory()).thenReturn("WATERMELON");
                when(request.getProductName()).thenReturn("수박 씨앗");
                when(request.getAmount()).thenReturn(100);
                when(request.getUnit()).thenReturn("BOX");
                when(request.getPrice()).thenReturn(new BigDecimal("50000"));
                when(request.getStatus()).thenReturn("SALE");
                when(request.getTags()).thenReturn(Map.of());

                Product savedProduct = Product.builder()
                                .productCode("WM001")
                                .productName("수박 씨앗")
                                .productCategory(ProductCategory.WATERMELON)
                                .amount(100)
                                .unit("BOX")
                                .price(new BigDecimal("50000"))
                                .status(ProductStatus.SALE)
                                .build();
                org.springframework.test.util.ReflectionTestUtils.setField(savedProduct, "id", 1L);

                when(productRepository.findTopByProductCategoryOrderByIdDesc(ProductCategory.WATERMELON))
                                .thenReturn(Optional.empty());
                when(productRepository.saveAndFlush(any(Product.class))).thenReturn(savedProduct);

                // when
                Long productId = productWriteService.createProduct(request);

                // then
                assertThat(productId).isEqualTo(1L);
                verify(productRepository, times(1)).saveAndFlush(any(Product.class));
        }

        @Test
        @DisplayName("상품 가격이 변경되면 수정과 함께 가격 변동 이력이 저장되어야 한다")
        void testUpdateProduct_PriceChanged() {
                // given
                Long productId = 1L;
                Long userId = 2L;

                Product product = Product.builder()
                                .productCode("WM001")
                                .productName("수박 씨앗")
                                .productCategory(ProductCategory.WATERMELON)
                                .amount(100)
                                .unit("BOX")
                                .price(new BigDecimal("50000")) // 기존 가격
                                .status(ProductStatus.SALE)
                                .build();

                Employee mockEmployee = Employee.builder()
                                .employeeName("테스터")
                                .build();
                User mockUser = mock(User.class);
                when(mockUser.getEmployee()).thenReturn(mockEmployee);

                ProductRequest request = mock(ProductRequest.class);
                when(request.getProductName()).thenReturn("수박 씨앗 플러스");
                when(request.getProductCategory()).thenReturn("WATERMELON");
                when(request.getProductDescription()).thenReturn("설명");
                when(request.getProductImageUrl()).thenReturn(null);
                when(request.getAmount()).thenReturn(100);
                when(request.getUnit()).thenReturn("BOX");
                when(request.getPrice()).thenReturn(new BigDecimal("60000")); // 새로운 가격
                when(request.getStatus()).thenReturn("SALE");
                when(request.getTags()).thenReturn(Map.of());

                when(productRepository.findById(productId)).thenReturn(Optional.of(product));
                when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

                // when
                productWriteService.updateProduct(productId, request, userId);

                // then
                assertThat(product.getPrice()).isEqualByComparingTo("60000"); // 엔티티 수정 확인
                verify(productPriceHistoryRepository, times(1)).save(any()); // 이력 저장 확인
        }

        @Test
        @DisplayName("상품 가격이 동일하면 이력이 저장되지 않는다")
        void testUpdateProduct_PriceNotChanged() {
                // given
                Long productId = 1L;
                Long userId = 2L;

                Product product = Product.builder()
                                .productCode("WM001")
                                .productName("수박 씨앗")
                                .productCategory(ProductCategory.WATERMELON)
                                .amount(100)
                                .unit("BOX")
                                .price(new BigDecimal("50000")) // 기존 가격
                                .status(ProductStatus.SALE)
                                .build();

                Employee mockEmployee = Employee.builder()
                                .employeeName("테스터")
                                .build();
                User mockUser = mock(User.class);
                when(mockUser.getEmployee()).thenReturn(mockEmployee);

                ProductRequest request = mock(ProductRequest.class);
                when(request.getProductName()).thenReturn("수박 씨앗 플러스");
                when(request.getProductCategory()).thenReturn("WATERMELON");
                when(request.getProductDescription()).thenReturn("설명");
                when(request.getProductImageUrl()).thenReturn(null);
                when(request.getAmount()).thenReturn(100);
                when(request.getUnit()).thenReturn("BOX");
                when(request.getPrice()).thenReturn(new BigDecimal("50000")); // 동일한 가격
                when(request.getStatus()).thenReturn("SALE");
                when(request.getTags()).thenReturn(Map.of());

                when(productRepository.findById(productId)).thenReturn(Optional.of(product));
                when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

                // when
                productWriteService.updateProduct(productId, request, userId);

                // then
                verify(productPriceHistoryRepository, never()).save(any()); // 이력 저장 호출 안 됨 확인
        }

        @Test
        @DisplayName("상품을 물리적으로 삭제 요청시 레포지토리의 삭제 메서드가 호출되어야 한다(Soft delete는 DB레이어에서 처리)")
        void testDeleteProduct() {
                // given
                Long productId = 1L;
                Product product = mock(Product.class);
                when(productRepository.findById(productId)).thenReturn(Optional.of(product));

                // when
                productWriteService.deleteProduct(productId);

                // then
                verify(productRepository, times(1)).delete(product);
        }
}
