package com.monsoon.seedflowplus.domain.note.service;

import com.monsoon.seedflowplus.domain.note.entity.SalesBriefing;
import com.monsoon.seedflowplus.domain.note.entity.SalesNote;
import com.monsoon.seedflowplus.domain.note.repository.SalesBriefingRepository;
import com.monsoon.seedflowplus.domain.note.repository.SalesNoteRepository;
import com.monsoon.seedflowplus.infra.ai.AiClient;
import com.monsoon.seedflowplus.infra.ai.rag.SalesNoteRagService;
import com.monsoon.seedflowplus.infra.ai.rag.ProductRagService;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BriefingService {

    private final SalesNoteRepository noteRepository;
    private final SalesBriefingRepository briefingRepository;
    private final AiClient aiClient;
    private final SalesNoteRagService salesNoteRagService;
    private final ProductRagService productRagService;

    // 고객별 동시성 제어를 위한 락 맵
    private final Map<Long, Lock> clientLocks = new ConcurrentHashMap<>();

    /**
     * 고객별 최신 브리핑 조회
     */
    public Optional<SalesBriefing> getBriefingByClient(Long clientId) {
        return briefingRepository.findByClientId(clientId);
    }

    /**
     * [RAG 리팩토링] 벡터 검색을 기반으로 AI 브리핑 비동기 갱신
     * 기존의 '영업 노트'뿐만 아니라 '종자 카탈로그' 정보도 함께 검색하여 전략을 수립합니다.
     */
    @Async("briefingTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refreshBriefingAsync(Long clientId) {
        Lock lock = clientLocks.computeIfAbsent(clientId, k -> new ReentrantLock());

        if (!lock.tryLock()) {
            log.info("이미 동일 고객에 대한 분석이 진행 중입니다. 스킵: clientId={}", clientId);
            return;
        }

        try {
            log.info("RAG 기반 비동기 AI 분석 프로세스 시작: clientId={}", clientId);

            // 1. 최소 데이터 확인 (최소 3개의 노트가 있어야 분석 가치 상정)
            if (noteRepository.countByClientId(clientId) < 3) {
                log.info("분석 데이터 부족(3개 미만)으로 브리핑 갱신을 취소합니다. clientId: {}", clientId);
                return;
            }

            // 2. [Retrieval] 벡터 저장소에서 관련 컨텍스트 검색
            String noteSearchQuery = "고객의 최근 비즈니스 현황, 불만 사항, 요구 사항 및 향후 영업 기회";
            List<TextSegment> noteContexts = salesNoteRagService.retrieveRelatedNotes(clientId, noteSearchQuery, 5);

            // [추가] 영업 기록 기반의 쿼리로 적합한 품종 카탈로그 검색
            String productSearchQuery = noteContexts.stream()
                    .map(TextSegment::text)
                    .collect(Collectors.joining(" "))
                    .substring(0, Math.min(500, noteContexts.stream().map(TextSegment::text).collect(Collectors.joining(" ")).length())); // 너무 길지 않게 자름
            
            List<TextSegment> productContexts = productRagService.retrieveRecommendedProducts(productSearchQuery, 3);

            if (noteContexts.isEmpty()) {
                log.warn("관련된 영업 노트를 찾을 수 없어 분석을 중단합니다. clientId: {}", clientId);
                return;
            }

            // 3. [Generation] AI 엔진 호출 (두 종류의 컨텍스트 전달)
            // noteContexts와 productContexts를 합쳐서 전달 (AiClient 인터페이스가 List<TextSegment>를 받으므로)
            List<TextSegment> combinedContexts = new java.util.ArrayList<>();
            combinedContexts.addAll(noteContexts);
            combinedContexts.addAll(productContexts);

            SalesBriefing analyzedResult = aiClient.analyzeSalesStrategy(clientId, combinedContexts);

            // 4. 결과 업데이트 또는 신규 저장
            SalesBriefing briefing = briefingRepository.findByClientId(clientId)
                    .map(existing -> {
                        existing.updateAnalysis(
                                analyzedResult.getStatusChange(),
                                analyzedResult.getLongTermPattern(),
                                analyzedResult.getEvidenceNoteIds(),
                                analyzedResult.getStrategySuggestion(),
                                analyzedResult.getVersion()
                        );
                        return existing;
                    })
                    .orElse(analyzedResult);

            briefingRepository.save(briefing);
            log.info("RAG 기반 비동기 AI 브리핑 갱신 성공: clientId={}", clientId);

        } catch (Exception e) {
            log.error("AI 브리핑 비동기 분석 중 오류 발생: clientId={}", clientId, e);
        } finally {
            lock.unlock();
        }
    }
}