package com.monsoon.seedflowplus.infra.ai.rag;

import com.monsoon.seedflowplus.domain.note.entity.SalesNote;
import com.monsoon.seedflowplus.domain.note.repository.SalesNoteRepository;
import dev.langchain4j.data.document.Document;
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
 * 영업 활동 기록(SalesNote)을 벡터화하여 관리하고 검색하는 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesNoteRagService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final SalesNoteRepository salesNoteRepository;

    /**
     * 애플리케이션 시작 시 기존의 모든 노트를 벡터 DB에 인덱싱합니다.
     */
    @PostConstruct
    public void initIndex() {
        log.info("기존 영업 노트 벡터 인덱싱 시작...");
        List<SalesNote> allNotes = salesNoteRepository.findAll();
        allNotes.forEach(this::indexNote);
        log.info("기존 {}건의 노트 인덱싱 완료.", allNotes.size());
    }

    /**
     * 개별 SalesNote를 벡터화하여 저장소에 인덱싱합니다.
     * Metadata: id, clientId, activityDate를 포함합니다.
     */
    public void indexNote(SalesNote note) {
        Metadata metadata = new Metadata()
                .add("type", "SALES_NOTE") // 데이터 타입 추가
                .add("id", note.getId())
                .add("clientId", note.getClientId())
                .add("activityDate", note.getActivityDate().toString());

        TextSegment segment = TextSegment.from(note.getContent(), metadata);
        Embedding embedding = embeddingModel.embed(segment).content();
        
        embeddingStore.add(embedding, segment);
    }

    /**
     * 특정 고객의 질문(Query)과 가장 관련성이 높은 과거 노트를 검색합니다.
     * @param clientId 검색 대상 고객 ID
     * @param query 검색어 (예: "최근 고객 불만 사항")
     * @param maxResults 최대 결과 개수
     * @return 관련성 높은 텍스트 세그먼트 리스트
     */
    public List<TextSegment> retrieveRelatedNotes(Long clientId, String query, int maxResults) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        // 특정 고객(clientId) 데이터로만 한정하는 필터 생성
        Filter filter = MetadataFilterBuilder.metadataKey("clientId").isEqualTo(clientId);

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .filter(filter)
                .maxResults(maxResults)
                .minScore(0.6) // 관련성 점수 임계치 설정
                .build();

        return embeddingStore.search(request).matches().stream()
                .map(EmbeddingMatch::embedded)
                .collect(Collectors.toList());
    }
}
