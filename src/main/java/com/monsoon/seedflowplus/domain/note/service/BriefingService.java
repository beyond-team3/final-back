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
    private final AiClient aiClient; // 인프라 계층의 AI 연동 클라이언트

    /**
     * 고객별 최신 브리핑 조회
     */
    public Optional<SalesBriefing> getBriefingByClient(Long clientId) {
        return briefingRepository.findByClientId(clientId);
    }

    /**
     * 누적 노트를 기반으로 AI 브리핑 생성 및 갱신
     * NoteService의 생성/수정/삭제 시점에 호출됩니다.
     */
    @Transactional
    public void refreshBriefing(Long clientId) {
        // 1. 분석을 위한 최근 노트 3개 추출 (비즈니스 로직 필수 조건)
        List<SalesNote> recentNotes = noteRepository.findTop3ByClientIdOrderByActivityDateDesc(clientId);

        if (recentNotes.size() < 3) {
            log.info("분석 데이터 부족으로 브리핑 갱신을 건너뜁니다. clientId: {}", clientId);
            return;
        }

        // 2. AI 분석용 텍스트 조립
        String combinedNotes = recentNotes.stream()
                .map(note -> String.format("[%s] %s", note.getActivityDate(), note.getContent()))
                .collect(Collectors.joining("\n---\n"));

        try {
            // 3. AI 엔진(Gemini 등)을 통한 전략 리포트 생성 요청
            // AiClient는 인프라 계층에서 상세 구현 (요약, 패턴, 전략 포함)
            SalesBriefing analyzedResult = aiClient.analyzeSalesStrategy(clientId, combinedNotes);

            // 4. 기존 브리핑이 있으면 업데이트, 없으면 신규 저장
            SalesBriefing briefing = briefingRepository.findByClientId(clientId)
                    .map(existing -> {
                        existing.updateAnalysis(
                                analyzedResult.getStatusChange(),
                                analyzedResult.getLongTermPattern(),
                                analyzedResult.getStrategySuggestion(),
                                analyzedResult.getVersion()
                        );
                        return existing;
                    })
                    .orElse(analyzedResult);

            briefingRepository.save(briefing);
            log.info("AI 브리핑 갱신 완료: clientId={}", clientId);

        } catch (Exception e) {
            log.error("AI 분석 중 오류 발생: clientId={}", clientId, e);
        }
    }
}