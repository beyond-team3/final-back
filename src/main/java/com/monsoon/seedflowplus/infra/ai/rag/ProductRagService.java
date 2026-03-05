package com.monsoon.seedflowplus.infra.ai.rag;

import com.monsoon.seedflowplus.domain.product.entity.Product;
import com.monsoon.seedflowplus.domain.product.repository.ProductRepository;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 종자 품종 정보(Product Catalog)를 벡터화하여 관리하고 검색하는 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRagService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ProductRepository productRepository;

    /**
     * 서버 기동 시 모든 종자 데이터를 벡터 DB에 동기화합니다.
     */
    @PostConstruct
    public void indexAllProducts() {
        log.info("종자 카탈로그 벡터 인덱싱 시작...");
        try {
            List<Product> products = productRepository.findAll();
            products.forEach(product -> {
                try {
                    this.indexProduct(product);
                } catch (Exception e) {
                    log.error("[RAG] 품종 인덱싱 실패 (ID: {}, Name: {}): {}", 
                            product.getId(), product.getProductName(), e.getMessage());
                }
            });
            log.info("총 {}건의 품종 정보 인덱싱 완료.", products.size());
        } catch (Exception e) {
            log.error("[RAG] 초기 품종 로딩 실패: {}", e.getMessage());
        }
    }

    /**
     * 개별 품종 정보를 벡터화하여 저장합니다.
     * Content: [상품명] + [상품 설명]
     * Metadata: type(PRODUCT_CATALOG), productId, category
     */
    public void indexProduct(Product product) {
        String combinedText = String.format("[%s] %s", 
                product.getProductName(), 
                product.getProductDescription());

        Metadata metadata = new Metadata()
                .add("type", "PRODUCT_CATALOG")
                .add("productId", product.getId())
                .add("category", product.getProductCategory().name());

        TextSegment segment = TextSegment.from(combinedText, metadata);
        Embedding embedding = embeddingModel.embed(segment).content();

        embeddingStore.add(embedding, segment);
    }

    /**
     * 주어진 검색어(영업 컨텍스트)와 가장 잘 맞는 추천 품종을 검색합니다.
     */
    public List<TextSegment> retrieveRecommendedProducts(String query, int maxResults) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        // PRODUCT_CATALOG 타입만 검색하도록 필터링
        Filter filter = MetadataFilterBuilder.metadataKey("type").isEqualTo("PRODUCT_CATALOG");

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .filter(filter)
                .maxResults(maxResults)
                .minScore(0.5) // 카탈로그 매칭을 위해 점수 기준을 약간 낮춤
                .build();

        return embeddingStore.search(request).matches().stream()
                .map(EmbeddingMatch::embedded)
                .collect(Collectors.toList());
    }
}
