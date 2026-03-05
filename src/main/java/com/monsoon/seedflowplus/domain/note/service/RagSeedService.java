package com.monsoon.seedflowplus.domain.note.service;

import com.monsoon.seedflowplus.domain.note.dto.response.RagSeedResponseDto;
import com.monsoon.seedflowplus.domain.note.entity.SalesBriefing;
import com.monsoon.seedflowplus.domain.note.repository.SalesBriefingRepository;
import com.monsoon.seedflowplus.domain.note.repository.SalesNoteRepository;
import com.monsoon.seedflowplus.infra.ai.AiClient;
import com.monsoon.seedflowplus.infra.ai.rag.ProductRagService;
import com.monsoon.seedflowplus.infra.ai.rag.SalesNoteRagService;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * RAGseed(랙씨드): 과거의 데이터(Seed)에서 최적의 전략을 인출(Retrieval)하는 통합 전략 엔진입니다.
 * '영업 데이터 자산(Seed)에서 추출한 전략 인출 엔진'이라는 브랜드 컨셉을 바탕으로 작동합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RagSeedService {

    private final SalesNoteRepository noteRepository;
    private final SalesBriefingRepository briefingRepository;
    private final AiClient aiClient;
    private final SalesNoteRagService salesNoteRagService;
    private final ProductRagService productRagService;

    private final Map<Long, Lock> clientLocks = new ConcurrentHashMap<>();

    /**
     * [이원화 로직 1] AI 영업 브리핑용 표준 리포트 생성
     */
    @Async("briefingTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refreshStandardBriefingAsync(Long clientId) {
        Lock lock = clientLocks.computeIfAbsent(clientId, k -> new ReentrantLock());
        if (!lock.tryLock()) return;

        try {
            log.info("[RAGseed] 표준 브리핑 갱신 시작: clientId={}", clientId);

            if (noteRepository.countByClientId(clientId) < 1) {
                log.info("[RAGseed] 데이터 부족으로 브리핑을 생성할 수 없습니다.");
                return;
            }

            // 표준 브리핑은 기본적으로 '고객별 모드'로 작동
            String scopeDesc = "특정 고객(ID: " + clientId + ")의 축적된 데이터 자산";
            List<TextSegment> noteContexts = salesNoteRagService.retrieveRelatedNotes(clientId, null, "전반적인 영업 현황", 5);
            
            String productQuery = noteContexts.stream().map(TextSegment::text).collect(Collectors.joining(" "));
            List<TextSegment> productContexts = productRagService.retrieveRecommendedProducts(productQuery, 3);

            List<TextSegment> combined = new ArrayList<>();
            combined.addAll(noteContexts);
            combined.addAll(productContexts);

            SalesBriefing analyzedResult = aiClient.analyzeSalesStrategy(clientId, combined, scopeDesc);

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
            log.info("[RAGseed] 표준 브리핑 갱신 완료");

        } finally {
            lock.unlock();
        }
    }

    /**
     * [이원화 로직 2] RAGseed 전용 맞춤형 전략 인출
     * 분석 범위(Scope)를 계층적으로 제어합니다.
     */
    public RagSeedResponseDto getTargetedStrategy(Long clientId, String contractId, String queryType) {
        log.info("[RAGseed] 타겟 전략 인출 요청: clientId={}, contractId={}, type={}", clientId, contractId, queryType);

        // 1. 분석 범위(Scope) 판별 및 설명 생성
        String scopeDesc;
        if (clientId != null && contractId != null && !contractId.isBlank() && !"NONE".equals(contractId)) {
            scopeDesc = String.format("특정 계약(코드: %s) 관련 데이터", contractId);
        } else if (clientId != null) {
            scopeDesc = String.format("특정 고객(ID: %d)의 전체 영업 데이터", clientId);
        } else {
            scopeDesc = "전사 영업 데이터 자산";
        }

        // 2. 쿼리 타입에 따른 '숨겨진 프롬프트' 설정
        String hiddenPrompt;
        String searchQuery;
        int maxResults = 5;

        switch (queryType.toUpperCase()) {
            case "RECAP":
                hiddenPrompt = "[RAGseed: 지난 맥락 인출] 선택된 범위 내의 최근 노트를 분석하여 핵심 결정 사항을 요약하라.";
                searchQuery = "최근 미팅 결정 사항 및 업무 진행 현황";
                break;
            case "RISK":
                hiddenPrompt = "[RAGseed: 리스크 탐지] 선택된 범위 내 데이터 중 클레임, 병해충 피해, 불만 사항 등 리스크를 추출하라.";
                searchQuery = "클레임 병해충 불만 경쟁사 리스크 문제";
                break;
            case "MATCHING":
                hiddenPrompt = "[RAGseed: 최적 종자 매칭] 분석 범위 내의 고객 선호도와 농가 환경을 바탕으로 최적 품종을 매칭하라.";
                searchQuery = "고객 선호 품종 및 재배 환경 특이사항";
                maxResults = 8;
                break;
            case "CHECKLIST":
                hiddenPrompt = "[RAGseed: 미팅 체크리스트] 선택된 범위 내에서 언급된 약속 사항 및 다음 방문 To-Do를 추출하라.";
                searchQuery = "약속 사항 향후 일정 확인 필요 사항";
                break;
            default:
                hiddenPrompt = "사용자 질의에 대해 최적의 답변을 인출하라: " + queryType;
                searchQuery = queryType;
        }

        // 3. 관련 컨텍스트 인출 (동적 필터 적용)
        List<TextSegment> noteContexts = salesNoteRagService.retrieveRelatedNotes(clientId, contractId, searchQuery, maxResults);
        List<TextSegment> productContexts = (queryType.equals("MATCHING")) 
                ? productRagService.retrieveRecommendedProducts(searchQuery, 5) 
                : List.of();

        List<TextSegment> combined = new ArrayList<>();
        combined.addAll(noteContexts);
        combined.addAll(productContexts);

        // 4. 엔진 호출 및 결과 반환
        String aiResponse = aiClient.generateTargetedResponse(hiddenPrompt, combined, scopeDesc);
        
        List<Long> evidenceIds = combined.stream()
                .map(s -> s.metadata().containsKey("id") 
                        ? Long.valueOf(s.metadata().get("id").toString()) 
                        : Long.valueOf(s.metadata().get("productId").toString()))
                .distinct()
                .collect(Collectors.toList());

        return RagSeedResponseDto.builder()
                .content(aiResponse)
                .evidenceIds(evidenceIds)
                .version("RAGseed-Targeted-v1.3")
                .attribution(String.format("Powered by RAGseed - %s 기반 분석", scopeDesc))
                .build();
    }

    /**
     * 고객별 브리핑 존재 여부 확인 및 조회
     */
    public Optional<SalesBriefing> getBriefingByClient(Long clientId) {
        return briefingRepository.findByClientId(clientId);
    }
}
