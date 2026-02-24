package com.monsoon.seedflowplus.domain.note.service;

import com.monsoon.seedflowplus.domain.note.entity.SalesBriefing;
import com.monsoon.seedflowplus.domain.note.entity.SalesNote;
import com.monsoon.seedflowplus.domain.note.repository.SalesBriefingRepository;
import com.monsoon.seedflowplus.domain.note.repository.SalesNoteRepository;
import com.monsoon.seedflowplus.domain.note.dto.NoteRequestDto;
import com.monsoon.seedflowplus.domain.note.dto.NoteSearchCondition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteService {

    private final SalesNoteRepository noteRepository;
    private final SalesBriefingRepository briefingRepository;
    private final BriefingService briefingService; // AI 분석 전담 서비스

    /**
     * 1. 영업 활동 기록 저장 및 AI 요약 분석
     * NoteView.vue의 '저장 및 AI 분석' 버튼 클릭 시 호출됩니다.
     */
    @Transactional
    public SalesNote createNote(NoteRequestDto dto) {
        // 엔티티 변환 및 저장
        SalesNote note = dto.toEntity();
        SalesNote savedNote = noteRepository.save(note);

        // 저장 후 해당 고객의 AI 브리핑 업데이트 트리거 (비동기 권장)
        updateBriefingAsync(savedNote.getClientId());

        return savedNote;
    }

    /**
     * 2. 영업 활동 기록 수정
     * NoteView.vue의 '수정 및 AI 재분석' 버튼 클릭 시 호출됩니다.
     */
    @Transactional
    public SalesNote updateNote(Long id, NoteRequestDto dto) {
        SalesNote note = noteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 노트를 찾을 수 없습니다. ID: " + id));

        // 엔티티 내부 로직을 통해 수정 (isEdited = true 처리 포함)
        note.updateNote(dto.getContent(), dto.getContractId(), dto.getActivityDate(), dto.getAiSummary());

        // 수정 시에도 브리핑 데이터 재분석 실행
        updateBriefingAsync(note.getClientId());

        return note;
    }

    /**
     * 3. 영업 활동 기록 삭제
     * NoteSearchView.vue에서 삭제 버튼 클릭 시 호출됩니다.
     */
    @Transactional
    public void deleteNote(Long id) {
        SalesNote note = noteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 노트를 찾을 수 없습니다."));

        Long clientId = note.getClientId();
        noteRepository.delete(note);

        // 삭제 후 데이터 변화에 따른 브리핑 재분석
        updateBriefingAsync(clientId);
    }

    /**
     * 4. 복합 필터링 검색
     * NoteSearchView.vue의 다양한 검색 조건을 처리합니다.
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
     * 내부 헬퍼 메서드: 브리핑 업데이트 로직 호출
     * 실제 분석 로직은 BriefingService에 위임하여 책임을 분리합니다.
     */
    private void updateBriefingAsync(Long clientId) {
        try {
            // 노트 개수가 3개 이상인지 확인 후 분석 실행
            long noteCount = noteRepository.countByClientId(clientId);
            if (noteCount >= 3) {
                briefingService.refreshBriefing(clientId);
            }
        } catch (Exception e) {
            log.error("AI 브리핑 갱신 중 오류 발생: clientId={}", clientId, e);
        }
    }
}