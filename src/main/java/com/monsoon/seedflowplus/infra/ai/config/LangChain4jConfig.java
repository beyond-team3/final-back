package com.monsoon.seedflowplus.infra.ai.config;

import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j를 이용한 RAG 설정을 담당하는 클래스입니다.
 */
@Configuration
public class LangChain4jConfig {

    @Value("${google.gemini.api.key}")
    private String geminiApiKey;

    /**
     * Gemini 채팅 모델 설정 (LangChain4j native integration)
     */
    @Bean
    public GoogleAiGeminiChatModel geminiChatModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName("gemini-1.5-flash")
                .temperature(0.1)
                .build();
    }

    /**
     * 로컬 환경에서 실행 가능한 AllMiniLmL6V2 임베딩 모델입니다.
     * 문장을 384차원의 벡터로 변환합니다.
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    /**
     * 로컬 테스트를 위한 인메모리 벡터 저장소입니다.
     * 실제 운영 환경에서는 Pinecone, Weaviate, PgVector 등을 권장합니다.
     */
    @Bean
    public EmbeddingStore embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }

    /**
     * 텍스트를 청크(Chunk) 단위로 분할하는 스플리터입니다.
     * 요구사항에 따라 Chunk Size의 10%를 Overlap으로 설정합니다.
     * (예: 500자 기준 50자 오버랩)
     */
    @Bean
    public DocumentSplitter documentSplitter() {
        return DocumentSplitters.recursive(500, 50);
    }
}
