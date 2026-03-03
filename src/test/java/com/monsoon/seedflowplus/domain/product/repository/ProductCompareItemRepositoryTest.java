package com.monsoon.seedflowplus.domain.product.repository;

import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.entity.ProductCategory;
import com.monsoon.seedflowplus.domain.product.entity.ProductCompare;
import com.monsoon.seedflowplus.domain.product.entity.ProductCompareItem;
import com.monsoon.seedflowplus.domain.product.entity.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import com.monsoon.seedflowplus.core.config.QuerydslConfig;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
class ProductCompareItemRepositoryTest {

    @Autowired
    private ProductCompareItemRepository productCompareItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductCompareRepository productCompareRepository;

    @Test
    @DisplayName("product id로 관련된 모든 비교 아이템을 삭제할 수 있다.")
    void deleteAllByProductIdTest() {
        // given
        Product product = Product.builder()
                .productCode("TEST-PD-01")
                .productName("테스트 상품")
                .productCategory(ProductCategory.WATERMELON)
                .amount(100)
                .unit("BOX")
                .price(new BigDecimal("10000"))
                .status(ProductStatus.SALE)
                .build();
        Product savedProduct = productRepository.save(product);

        ProductCompare productCompare = ProductCompare.builder()
                .title("테스트 비교 내역")
                .build();
        ProductCompare savedCompare = productCompareRepository.save(productCompare);

        ProductCompareItem item1 = ProductCompareItem.builder().product(savedProduct).build();
        item1.setProductCompare(savedCompare);
        ProductCompareItem item2 = ProductCompareItem.builder().product(savedProduct).build();
        item2.setProductCompare(savedCompare);

        productCompareItemRepository.saveAll(List.of(item1, item2));
        productCompareItemRepository.flush();

        // when
        productCompareItemRepository.deleteAllByProductId(savedProduct.getId());
        productCompareItemRepository.flush();

        // then
        List<ProductCompareItem> remainingItems = productCompareItemRepository.findAll();
        assertThat(remainingItems).isEmpty();
    }
}
