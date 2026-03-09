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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    @PostConstruct
    public void initIndex() {
        log.info("기존 영업 노트 벡터 인덱싱 시작...");
        try {
            int pageSize = 100;
            int pageNumber = 0;
            long totalIndexed = 0;
            Page<SalesNote> page;

            do {
                page = salesNoteRepository.findAll(PageRequest.of(pageNumber, pageSize));
                for (SalesNote note : page.getContent()) {
                    try {
                        this.indexNote(note);
                        totalIndexed++;
                    } catch (Exception e) {
                        log.error("[RAG] 노트 인덱싱 실패 (ID: {}): {}", note.getId(), e.getMessage());
                    }
                }
                log.info("[RAG] 노트 인덱싱 진행 중... (현재까지: {}건)", totalIndexed);
                pageNumber++;
            } while (page.hasNext());

            log.info("기존 {}건의 노트 인덱싱 완료.", totalIndexed);
        } catch (Exception e) {
            log.error("[RAG] 초기 노트 로딩 실패: {}", e.getMessage());
        }
    }

    /**
     * 개별 SalesNote를 벡터화하여 저장소에 인덱싱합니다.
     * [개선] 중복 방지를 위해 기존 정보를 삭제하되, 신규 임베딩이 성공한 후에만 삭제를 수행하여 데이터 유실을 방지합니다.
     */
    public void indexNote(SalesNote note) {
        Metadata metadata = new Metadata()
                .add("type", "SALES_NOTE")
                .add("id", note.getId().toString())
                .add("clientId", note.getClientId().toString())
                .add("contractId", note.getContractId() != null ? note.getContractId() : "NONE")
                .add("activityDate", note.getActivityDate().toString());

        TextSegment segment = TextSegment.from(note.getContent(), metadata);
        
        // 1. 임베딩 생성 (실패 시 예외 발생, 이후 삭제 로직 실행 안 됨)
        Embedding embedding = embeddingModel.embed(segment).content();
        
        // 2. 임베딩 성공 시 기존 데이터 삭제 (Atomic-ish Update 효과)
        this.deleteNote(note.getId());
        
        // 3. 신규 데이터 추가
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
