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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * RAGseed(랙씨드): 과거의 데이터(Seed)에서 최적의 전략을 인출(Retrieval)하는 통합 전략 엔진입니다.
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
        lock.lock();

        try {
            log.info("[RAGseed] 표준 브리핑 갱신 시작: clientId={}", clientId);

            if (noteRepository.countByClientId(clientId) < 1) {
                log.info("[RAGseed] 데이터 부족으로 브리핑을 생성할 수 없습니다.");
                return;
            }

            String scopeDesc = "특정 고객(ID: " + clientId + ")의 축적된 데이터 자산";
            List<TextSegment> noteContexts = salesNoteRagService.retrieveRelatedNotes(clientId, null, "전반적인 영업 현황", 5);
            
            String productQuery = noteContexts.stream().map(TextSegment::text).collect(Collectors.joining(" "));
            List<TextSegment> productContexts = productRagService.retrieveRecommendedProducts(productQuery, 3);

            List<TextSegment> combined = new ArrayList<>();
            combined.addAll(noteContexts);
            combined.addAll(productContexts);

            SalesBriefing analyzedResult = aiClient.analyzeSalesStrategy(clientId, combined, scopeDesc);

            // [교차 검증 필터] AI가 반환한 근거 ID 중 실제 컨텍스트(노트)에 존재하는 ID만 필터링
            Set<Long> validContextIds = extractIdsFromSegments(noteContexts);

            List<Long> rawEvidenceIds = analyzedResult.getEvidenceNoteIds();
            List<Long> verifiedEvidenceIds = (rawEvidenceIds != null)
                    ? rawEvidenceIds.stream()
                        .filter(validContextIds::contains)
                        .collect(Collectors.toList())
                    : new ArrayList<>();

            SalesBriefing briefing = briefingRepository.findByClientId(clientId)
                    .map(existing -> {
                        existing.updateAnalysis(
                                analyzedResult.getStatusChange(),
                                analyzedResult.getLongTermPattern(),
                                verifiedEvidenceIds, // 검증된 ID만 반영
                                analyzedResult.getStrategySuggestion()
                        );
                        return existing;
                    })
                    .orElse(SalesBriefing.builder()
                            .clientId(analyzedResult.getClientId())
                            .statusChange(analyzedResult.getStatusChange())
                            .longTermPattern(analyzedResult.getLongTermPattern())
                            .evidenceNoteIds(verifiedEvidenceIds) // 검증된 ID만 반영
                            .strategySuggestion(analyzedResult.getStrategySuggestion())
                            .build());

            briefingRepository.save(briefing);
            log.info("[RAGseed] 표준 브리핑 갱신 완료 (검증된 근거 개수: {})", verifiedEvidenceIds.size());

        } finally {
            lock.unlock();
        }
    }

    /**
     * [이원화 로직 2] RAGseed 전용 맞춤형 전략 인출
     */
    public RagSeedResponseDto getTargetedStrategy(Long clientId, String contractId, String queryType) {
        log.info("[RAGseed] 타겟 전략 인출 요청: clientId={}, contractId={}, type={}", clientId, contractId, queryType);

        String scopeDesc = (contractId != null && !contractId.isBlank() && !"NONE".equals(contractId))
                ? String.format("특정 계약(코드: %s) 관련 데이터", contractId)
                : String.format("특정 고객(ID: %d)의 전체 영업 데이터", clientId);

        String normalizedQueryType = (queryType == null) ? "" : queryType.trim();
        String hiddenPrompt;
        String searchQuery;
        int maxResults = 5;

        switch (normalizedQueryType.toUpperCase()) {
            case "RECAP":
                hiddenPrompt = "[RAGseed: 지난 맥락 인출] 선택된 범위 내의 최근 노트를 분석하여 핵심 결정 사항을 요약하라.\n반드시 다음 JSON 구조로만 답변하세요: { \"content\": \"마크다운 형식의 요약 내용\" }";
                searchQuery = "최근 미팅 결정 사항 및 업무 진행 현황";
                break;
            case "RISK":
                hiddenPrompt = "[RAGseed: 리스크 탐지] 선택된 범위 내 데이터 중 클레임, 병해충 피해, 불만 사항 등 리스크를 추출하라.\n반드시 다음 JSON 구조로만 답변하세요: { \"content\": \"마크다운 형식의 탐지된 리스크 상세 내용\" }";
                searchQuery = "클레임 병해충 불만 경쟁사 리스크 문제";
                break;
            case "MATCHING":
                hiddenPrompt = "[RAGseed: 최적 종자 매칭] 분석 범위 내의 고객 선호도와 농가 환경을 바탕으로 최적 품종을 매칭하라.\n반드시 다음 JSON 구조로만 답변하세요: { \"content\": \"마크다운 형식의 종자 추천 내용\" }";
                searchQuery = "고객 선호 품종 및 재배 환경 특이사항";
                maxResults = 8;
                break;
            case "CHECKLIST":
                hiddenPrompt = "[RAGseed: 미팅 체크리스트] 선택된 범위 내에서 언급된 약속 사항 및 다음 방문 To-Do를 추출하라.\n반드시 다음 JSON 구조로만 답변하세요: { \"content\": \"마크다운 형식의 체크리스트 내용\" }";
                searchQuery = "약속 사항 향후 일정 확인 필요 사항";
                break;
            default:
                hiddenPrompt = "사용자 질의에 대해 최적의 답변을 인출하라: " + normalizedQueryType + "\n반드시 다음 JSON 구조로만 답변하세요: { \"content\": \"마크다운 형식의 사용자 질의에 대한 답변\" }";
                searchQuery = normalizedQueryType;
        }

        List<TextSegment> noteContexts = salesNoteRagService.retrieveRelatedNotes(clientId, contractId, searchQuery, maxResults);
        List<TextSegment> productContexts = (normalizedQueryType.equalsIgnoreCase("MATCHING"))
                ? productRagService.retrieveRecommendedProducts(searchQuery, 5)
                : List.of();

        List<TextSegment> combined = new ArrayList<>();
        combined.addAll(noteContexts);
        combined.addAll(productContexts);

        if (combined.isEmpty()) {
            hiddenPrompt += "\n[주의] 현재 분석 범위에 영업 기록이 전혀 없습니다. '해당 고객에 대한 영업 기록이 없어 분석이 불가능합니다. 먼저 노트를 작성해주세요.'라고 답변하세요.";
        }

        String aiResponse = aiClient.generateTargetedResponse(hiddenPrompt, combined, scopeDesc);
        
        // 실제 인출된 데이터의 ID들을 결과에 포함 (노트 ID만 추출하도록 수정)
        List<Long> evidenceIds = new ArrayList<>(extractIdsFromSegments(noteContexts));

        return RagSeedResponseDto.builder()
                .content(aiResponse)
                .evidenceIds(evidenceIds)
                .attribution(String.format("Powered by RAGseed - %s 기반 분석", scopeDesc))
                .build();
    }

    /**
     * TextSegment 리스트에서 유효한 ID(노트 ID 또는 상품 ID) 세트를 추출합니다.
     */
    private Set<Long> extractIdsFromSegments(List<TextSegment> segments) {
        return segments.stream()
                .map(s -> {
                    String idStr = s.metadata().containsKey("id")
                            ? s.metadata().get("id").toString()
                            : (s.metadata().containsKey("productId") ? s.metadata().get("productId").toString() : null);
                    if (idStr == null) return null;
                    try {
                        return Long.valueOf(idStr);
                    } catch (NumberFormatException e) {
                        log.warn("[RAGseed] 유효하지 않은 ID 형식 무시됨: {}", idStr);
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 고객별 브리핑 존재 여부 확인 및 조회
     */
    public Optional<SalesBriefing> getBriefingByClient(Long clientId) {
        return briefingRepository.findByClientId(clientId);
    }
}
