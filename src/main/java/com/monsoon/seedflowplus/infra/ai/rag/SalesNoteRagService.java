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
                .add("type", "SALES_NOTE")
                .add("id", note.getId().toString())
                .add("clientId", note.getClientId().toString())
                .add("contractId", note.getContractId() != null ? note.getContractId() : "NONE") // [추가] 계약 ID 메타데이터
                .add("activityDate", note.getActivityDate().toString());

        TextSegment segment = TextSegment.from(note.getContent(), metadata);
        Embedding embedding = embeddingModel.embed(segment).content();
        
        embeddingStore.add(embedding, segment);
    }

    /**
     * 특정 노트를 벡터 DB에서 삭제합니다. (업데이트/삭제 시 사용)
     */
    public void deleteNote(Long noteId) {
        Filter filter = MetadataFilterBuilder.metadataKey("id").isEqualTo(noteId.toString());
        embeddingStore.removeAll(filter);
    }

    /**
     * 계층적 분석 범위를 지원하는 검색 메서드
     * @param clientId (필수) 고객 ID
     * @param contractId (선택) 계약 코드
     * @param query 검색어
     */
    public List<TextSegment> retrieveRelatedNotes(Long clientId, String contractId, String query, int maxResults) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        // [동적 필터 생성 로직] clientId는 항상 필수
        Filter filter;
        if (contractId != null && !contractId.isBlank() && !"NONE".equals(contractId)) {
            // 1. 계약별 모드: 고객 ID와 계약 코드가 모두 일치해야 함
            filter = MetadataFilterBuilder.metadataKey("clientId").isEqualTo(clientId.toString())
                    .and(MetadataFilterBuilder.metadataKey("contractId").isEqualTo(contractId));
        } else {
            // 2. 고객별 모드: 해당 고객의 모든 데이터
            filter = MetadataFilterBuilder.metadataKey("clientId").isEqualTo(clientId.toString());
        }

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .filter(filter)
                .maxResults(maxResults)
                .minScore(0.5)
                .build();

        return embeddingStore.search(request).matches().stream()
                .map(EmbeddingMatch::embedded)
                .collect(Collectors.toList());
    }
}
