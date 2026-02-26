package com.monsoon.seedflowplus.domain.note.service;

import com.monsoon.seedflowplus.domain.note.entity.SalesBriefing;
import com.monsoon.seedflowplus.domain.note.entity.SalesNote;
import com.monsoon.seedflowplus.domain.note.repository.SalesBriefingRepository;
import com.monsoon.seedflowplus.domain.note.repository.SalesNoteRepository;
import com.monsoon.seedflowplus.infra.ai.AiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BriefingService {

    private final SalesNoteRepository noteRepository;
    private final SalesBriefingRepository briefingRepository;
    private final AiClient aiClient;

    /**
     * 고객별 최신 브리핑 조회
     */
    public Optional<SalesBriefing> getBriefingByClient(Long clientId) {
        return briefingRepository.findByClientId(clientId);
    }

    /**
     * 누적 노트를 기반으로 AI 브리핑 생성 및 갱신
     */
    @Transactional
    public void refreshBriefing(Long clientId) {
        // 1. 분석을 위한 최근 노트 3개 추출 (비즈니스 로직 조건 유지)
        List<SalesNote> recentNotes = noteRepository.findTop3ByClientIdOrderByActivityDateDescIdDesc(clientId);

        if (recentNotes.size() < 3) {
            log.info("분석 데이터 부족으로 브리핑 갱신을 건너뜁니다. clientId: {}", clientId);
            return;
        }

        // 2. AI 분석용 텍스트 조립 (근거 추출을 위해 ID를 명시적으로 포함)
        String combinedNotes = recentNotes.stream()
                .map(note -> String.format("[ID: %d] (%s) %s",
                        note.getId(), note.getActivityDate(), note.getContent()))
                .collect(Collectors.joining("\n---\n"));

        try {
            // 3. AI 엔진을 통한 전략 분석 호출
            // 이제 analyzedResult에는 evidenceNoteIds가 포함되어 반환됩니다.
            SalesBriefing analyzedResult = aiClient.analyzeSalesStrategy(clientId, combinedNotes);
            if (analyzedResult == null) {
                log.warn("AI 분석 결과가 null입니다: clientId={}", clientId);
                return;

                // 4. 기존 데이터 업데이트 또는 신규 저장
            SalesBriefing briefing = briefingRepository.findByClientId(clientId)
                    .map(existing -> {
                        // 엔티티의 수정된 updateAnalysis 메서드 호출 (evidenceNoteIds 파라미터 추가)
                        existing.updateAnalysis(
                                analyzedResult.getStatusChange(),
                                analyzedResult.getLongTermPattern(),
                                analyzedResult.getEvidenceNoteIds(), // 근거 ID 리스트 반영
                                analyzedResult.getStrategySuggestion(),
                                analyzedResult.getVersion()
                        );
                        return existing;
                    })
                    .orElse(analyzedResult);

            briefingRepository.save(briefing);
            log.info("AI 브리핑 갱신 완료 (근거 포함): clientId={}", clientId);

        } catch (Exception e) {
            log.error("AI 브리핑 갱신 중 오류 발생: clientId={}", clientId, e);
        }
    }
}