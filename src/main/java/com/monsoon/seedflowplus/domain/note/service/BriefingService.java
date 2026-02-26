package com.monsoon.seedflowplus.domain.note.service;

import com.monsoon.seedflowplus.domain.note.entity.SalesBriefing;
import com.monsoon.seedflowplus.domain.note.entity.SalesNote;
import com.monsoon.seedflowplus.domain.note.repository.SalesBriefingRepository;
import com.monsoon.seedflowplus.domain.note.repository.SalesNoteRepository;
import com.monsoon.seedflowplus.infra.ai.AiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
     * [리팩토링] 누적 노트를 기반으로 AI 브리핑 비동기 갱신
     * @Async: 별도의 TaskExecutor 스레드에서 실행되어 NoteService의 응답을 방해하지 않음
     * REQUIRES_NEW: 부모 트랜잭션(노트 저장)과 별개로 새로운 트랜잭션에서 분석 결과를 커밋함
     */
    @Async("briefingTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void refreshBriefingAsync(Long clientId) {
        log.info("비동기 AI 분석 프로세스 시작: clientId={}", clientId);

        // 1. 분석 데이터 유효성 검사 (최근 노트 3개 추출)
        List<SalesNote> recentNotes = noteRepository.findTop3ByClientIdOrderByActivityDateDescIdDesc(clientId);

        if (recentNotes.size() < 3) {
            log.info("분석 데이터 부족(3개 미만)으로 브리핑 갱신을 취소합니다. clientId: {}", clientId);
            return;
        }

        // 2. AI 분석용 텍스트 조립
        String combinedNotes = recentNotes.stream()
                .map(note -> String.format("[ID: %d] (%s) %s",
                        note.getId(), note.getActivityDate(), note.getContent()))
                .collect(Collectors.joining("\n---\n"));

        try {
            // 3. AI 엔진 호출 (네트워크 I/O 발생 - 타임아웃 설정 적용됨)
            SalesBriefing analyzedResult = aiClient.analyzeSalesStrategy(clientId, combinedNotes);

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
            log.info("비동기 AI 브리핑 갱신 성공: clientId={}", clientId);

        } catch (Exception e) {
            // 비동기 스레드 내에서도 clientId를 포함한 정확한 로깅 유지
            log.error("AI 브리핑 비동기 분석 중 오류 발생: clientId={}", clientId, e);
        }
    }
}