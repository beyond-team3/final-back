package com.monsoon.seedflowplus.domain.note.service;

import com.monsoon.seedflowplus.domain.note.entity.SalesNote;
import com.monsoon.seedflowplus.domain.note.repository.SalesNoteRepository;
import com.monsoon.seedflowplus.domain.note.dto.NoteRequestDto;
import com.monsoon.seedflowplus.domain.note.dto.NoteSearchCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteService {

    private final SalesNoteRepository noteRepository;
    private final BriefingService briefingService;

    /**
     * 1. 영업 활동 기록 목록 조회 및 검색
     */
    public List<SalesNote> searchNotes(NoteSearchCondition condition) {
        return noteRepository.searchNotes(
                condition.getClientId(),
                condition.getContractId(),
                condition.getKeyword(),
                condition.getDateFrom(),
                condition.getDateTo(),
                condition.getSort()
        );
    }

    /**
     * 2. 영업 활동 기록 저장 및 AI 요약 분석 트리거
     */
    @Transactional
    public SalesNote createNote(NoteRequestDto dto) {
        Long currentUserId = getCurrentUserId();
        SalesNote note = dto.toEntity(currentUserId);
        SalesNote savedNote = noteRepository.save(note);

        // [리팩토링] 비동기 분석 트리거
        triggerBriefingUpdate(savedNote.getClientId());

        return savedNote;
    }

    /**
     * 3. 영업 활동 기록 수정 및 AI 재분석 트리거
     */
    @Transactional
    public SalesNote updateNote(Long id, NoteRequestDto dto) {
        SalesNote note = noteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 노트를 찾을 수 없습니다. ID: " + id));

        note.updateNote(dto.getContent(), dto.getContractId(), dto.getActivityDate(), dto.getAiSummary());

        // [리팩토링] 데이터 수정 후 비동기 분석 요청
        triggerBriefingUpdate(note.getClientId());

        return note;
    }

    /**
     * 4. 영업 활동 기록 삭제 및 AI 재분석 트리거
     */
    @Transactional
    public void deleteNote(Long id) {
        SalesNote note = noteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 노트를 찾을 수 없습니다."));

        Long clientId = note.getClientId();
        noteRepository.delete(note);

        // [리팩토링] 데이터 삭제 후 비동기 분석 요청
        triggerBriefingUpdate(clientId);
    }

    /**
     * [리팩토링] 내부 헬퍼 메서드: 진정한 비동기 호출 수행
     * 메서드명을 의도에 맞게 'trigger'로 변경하고 실제 비동기 메서드를 호출함
     */
    private void triggerBriefingUpdate(Long clientId) {
        // 현재 실행 중인 스레드가 트랜잭션 상태인지 확인
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // 트랜잭션 동기화 매니저에 '커밋 후 실행' 콜백 등록
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 실제 DB 커밋이 완료된 직후에만 비동기 호출 실행
                    log.info("트랜잭션 커밋 완료 확인 - 비동기 분석 트리거: clientId={}", clientId);
                    briefingService.refreshBriefingAsync(clientId);
                }
            });
        } else {
            // 트랜잭션이 없는 환경(예: 테스트 코드 등)에서는 즉시 호출
            briefingService.refreshBriefingAsync(clientId);
        }
    }

    private Long getCurrentUserId() {
        return 123L; // 예시 ID (Spring Security 연동 필요)
    }
}