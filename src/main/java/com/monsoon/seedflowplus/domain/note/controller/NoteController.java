package com.monsoon.seedflowplus.domain.note.controller;

import com.monsoon.seedflowplus.domain.note.dto.NoteRequestDto;
import com.monsoon.seedflowplus.domain.note.dto.NoteSearchCondition;
import com.monsoon.seedflowplus.domain.note.entity.SalesNote;
import com.monsoon.seedflowplus.domain.note.entity.SalesBriefing;
import com.monsoon.seedflowplus.domain.note.service.NoteService;
import com.monsoon.seedflowplus.domain.note.service.BriefingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final BriefingService briefingService;

    /**
     * 1. 영업 노트 목록 조회 및 검색
     * NoteSearchView.vue의 필터링 및 검색 기능을 담당합니다.
     */
    @GetMapping
    public ResponseEntity<List<SalesNote>> getNotes(NoteSearchCondition condition) {
        List<SalesNote> notes = noteService.searchNotes(condition); //
        return ResponseEntity.ok(notes);
    }

    /**
     * 2. 영업 노트 상세 저장 및 AI 요약 분석
     * NoteView.vue에서 '저장' 버튼 클릭 시 호출됩니다.
     */
    @PostMapping
    public ResponseEntity<SalesNote> createNote(@RequestBody NoteRequestDto dto) {
        SalesNote createdNote = noteService.createNote(dto); //
        return ResponseEntity.status(HttpStatus.CREATED).body(createdNote);
    }

    /**
     * 3. 영업 활동 기록 수정 및 AI 재분석
     * NoteView.vue에서 '수정' 버튼 클릭 시 호출됩니다.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SalesNote> updateNote(
            @PathVariable Long id,
            @RequestBody NoteRequestDto dto) {
        SalesNote updatedNote = noteService.updateNote(id, dto); //
        return ResponseEntity.ok(updatedNote);
    }

    /**
     * 4. 영업 활동 기록 삭제
     * NoteSearchView.vue에서 '삭제' 버튼 클릭 시 호출됩니다.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id) {
        noteService.deleteNote(id); //
        return ResponseEntity.noContent().build();
    }

    /**
     * 5. AI 영업 브리핑 조회
     * NoteBriefingView.vue에서 고객 선택 시 전략 리포트를 반환합니다.
     */
    @GetMapping("/briefing/{clientId}")
    public ResponseEntity<SalesBriefing> getBriefing(@PathVariable Long clientId) {
        //에서 정의된 고객별 브리핑 조회 로직 호출
        return briefingService.getBriefingByClient(clientId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}