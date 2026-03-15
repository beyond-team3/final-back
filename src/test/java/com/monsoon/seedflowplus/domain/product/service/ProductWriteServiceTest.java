package com.monsoon.seedflowplus.domain.product.service;

import com.monsoon.seedflowplus.domain.account.entity.Employee;
import com.monsoon.seedflowplus.domain.account.entity.Role;
import com.monsoon.seedflowplus.domain.account.entity.Status;
import com.monsoon.seedflowplus.domain.account.entity.User;
import com.monsoon.seedflowplus.domain.account.repository.UserRepository;
import com.monsoon.seedflowplus.domain.notification.event.NotificationEventPublisher;
import com.monsoon.seedflowplus.domain.notification.event.ProductCreatedEvent;
import com.monsoon.seedflowplus.domain.product.dto.request.ProductRequest;
import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.ProductCategory;
import com.monsoon.seedflowplus.domain.product.entity.ProductStatus;
import com.monsoon.seedflowplus.domain.product.repository.ProductBookmarkRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductPriceHistoryRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductTagRepository;
import com.monsoon.seedflowplus.domain.product.repository.CultivationTimeRepository;
import com.monsoon.seedflowplus.domain.product.repository.ProductCompareItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.multipart.MultipartFile;
import com.monsoon.seedflowplus.infra.aws.service.S3UploadService;

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
        private ProductCompareItemRepository productCompareItemRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private S3UploadService s3UploadService;
        @Mock
        private NotificationEventPublisher notificationEventPublisher;

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
                when(userRepository.findAllByRole(Role.SALES_REP))
                                .thenReturn(java.util.List.of(salesRepUser(100L), salesRepUser(200L)));

                // when (이미지 없는 경우)
                Long productId = productWriteService.createProduct(request, null);

                // then
                assertThat(productId).isEqualTo(1L);
                verify(productRepository, times(1)).saveAndFlush(any(Product.class));
                verify(s3UploadService, never()).uploadProductImage(any());
                verify(notificationEventPublisher, times(2)).publishAfterCommit(any(ProductCreatedEvent.class));
        }

        @Test
        @DisplayName("이미지 파일과 함께 신규 상품을 등록하면 S3 업로드 서비스가 호출되어야 한다")
        void testCreateProductWithImage() {
                // given
                ProductRequest request = mock(ProductRequest.class);
                when(request.getProductCategory()).thenReturn("WATERMELON");
                when(request.getProductName()).thenReturn("수박 씨앗");
                when(request.getAmount()).thenReturn(100);
                when(request.getUnit()).thenReturn("BOX");
                when(request.getPrice()).thenReturn(new BigDecimal("50000"));
                when(request.getStatus()).thenReturn("SALE");
                when(request.getTags()).thenReturn(Map.of());

                MultipartFile mockImage = mock(MultipartFile.class);
                when(mockImage.isEmpty()).thenReturn(false);
                String expectedImageUrl = "https://s3.aws.com/products/image.jpg";
                when(s3UploadService.uploadProductImage(mockImage)).thenReturn(expectedImageUrl);

                Product savedProduct = Product.builder()
                                .productCode("WM002")
                                .productName("수박 씨앗")
                                .productCategory(ProductCategory.WATERMELON)
                                .amount(100)
                                .unit("BOX")
                                .price(new BigDecimal("50000"))
                                .status(ProductStatus.SALE)
                                .productImageUrl(expectedImageUrl)
                                .build();
                org.springframework.test.util.ReflectionTestUtils.setField(savedProduct, "id", 2L);

                when(productRepository.findTopByProductCategoryOrderByIdDesc(ProductCategory.WATERMELON))
                                .thenReturn(Optional.empty());
                when(productRepository.saveAndFlush(any(Product.class))).thenReturn(savedProduct);
                when(userRepository.findAllByRole(Role.SALES_REP)).thenReturn(java.util.List.of());

                // when
                Long productId = productWriteService.createProduct(request, mockImage);

                // then
                assertThat(productId).isEqualTo(2L);
                verify(s3UploadService, times(1)).uploadProductImage(mockImage);
                verify(productRepository, times(1)).saveAndFlush(any(Product.class));
        }

        @Test
        @DisplayName("이미지 업로드 후 데이터 무결성 예외(중복 등) 발생 시 S3 이미지가 삭제되어야 한다")
        void testCreateProductWithImage_DuplicateCode() {
                // given
                ProductRequest request = mock(ProductRequest.class);
                when(request.getProductCategory()).thenReturn("WATERMELON");
                when(request.getProductName()).thenReturn("수박 씨앗");
                when(request.getAmount()).thenReturn(100);
                when(request.getUnit()).thenReturn("BOX");
                when(request.getPrice()).thenReturn(new BigDecimal("50000"));
                when(request.getStatus()).thenReturn("SALE");
                when(request.getTags()).thenReturn(Map.of());

                MultipartFile mockImage = mock(MultipartFile.class);
                when(mockImage.isEmpty()).thenReturn(false);
                String expectedImageUrl = "https://s3.aws.com/products/image.jpg";
                when(s3UploadService.uploadProductImage(mockImage)).thenReturn(expectedImageUrl);

                when(productRepository.findTopByProductCategoryOrderByIdDesc(ProductCategory.WATERMELON))
                                .thenReturn(Optional.empty());

                when(productRepository.saveAndFlush(any(Product.class)))
                                .thenThrow(org.springframework.dao.DataIntegrityViolationException.class);

                // when & then
                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> productWriteService.createProduct(request, mockImage))
                                .isInstanceOf(com.monsoon.seedflowplus.core.common.support.error.CoreException.class)
                                .hasFieldOrPropertyWithValue("errorType",
                                                com.monsoon.seedflowplus.core.common.support.error.ErrorType.DUPLICATE_PRODUCT_CODE);

                verify(s3UploadService, times(1)).uploadProductImage(mockImage);
                verify(s3UploadService, times(1)).deleteImageFromUrl(expectedImageUrl);
        }

        private User salesRepUser(Long id) {
                User user = User.builder()
                                .loginId("sales-" + id)
                                .loginPw("pw")
                                .status(Status.ACTIVATE)
                                .role(Role.SALES_REP)
                                .build();
                org.springframework.test.util.ReflectionTestUtils.setField(user, "id", id);
                return user;
        }

        @Test
        @DisplayName("이미지 업로드 후 알 수 없는 예외 발생 시 원래 예외가 던져지고 S3 이미지는 삭제되어야 한다")
        void testCreateProductWithImage_GenericException() {
                // given
                ProductRequest request = mock(ProductRequest.class);
                when(request.getProductCategory()).thenReturn("WATERMELON");
                when(request.getProductName()).thenReturn("수박 씨앗");
                when(request.getAmount()).thenReturn(100);
                when(request.getUnit()).thenReturn("BOX");
                when(request.getPrice()).thenReturn(new BigDecimal("50000"));
                when(request.getStatus()).thenReturn("SALE");
                when(request.getTags()).thenReturn(Map.of());

                MultipartFile mockImage = mock(MultipartFile.class);
                when(mockImage.isEmpty()).thenReturn(false);
                String expectedImageUrl = "https://s3.aws.com/products/image.jpg";
                when(s3UploadService.uploadProductImage(mockImage)).thenReturn(expectedImageUrl);

                when(productRepository.findTopByProductCategoryOrderByIdDesc(ProductCategory.WATERMELON))
                                .thenReturn(Optional.empty());

                when(productRepository.saveAndFlush(any(Product.class)))
                                .thenThrow(new RuntimeException("DB 알 수 없는 에러"));

                // when & then
                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> productWriteService.createProduct(request, mockImage))
                                .isInstanceOf(RuntimeException.class)
                                .hasMessage("DB 알 수 없는 에러");

                verify(s3UploadService, times(1)).uploadProductImage(mockImage);
                verify(s3UploadService, times(1)).deleteImageFromUrl(expectedImageUrl);
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
                productWriteService.updateProduct(productId, request, null, userId);

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
                productWriteService.updateProduct(productId, request, null, userId);

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
