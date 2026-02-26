package com.monsoon.seedflowplus.domain.note.service;

import com.monsoon.seedflowplus.domain.note.dto.NoteRequestDto;
import com.monsoon.seedflowplus.domain.note.entity.SalesNote;
import com.monsoon.seedflowplus.domain.note.repository.SalesNoteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @InjectMocks
    private NoteService noteService;

    @Mock
    private SalesNoteRepository noteRepository;

    @Mock
    private BriefingService briefingService;

    @Test
    @DisplayName("새로운 노트를 저장하면 브리핑 갱신 로직이 트리거되어야 한다")
    void createNote_TriggersBriefingUpdate() {
        // Given
        Long mockUserId = 100L; // 작성자 ID 가정
        NoteRequestDto dto = NoteRequestDto.builder()
                .clientId(1L)
                .content("미팅 내용")
                .build();

        // toEntity 호출 시 필요한 인자(mockUserId)를 전달하도록 수정
        SalesNote note = dto.toEntity(mockUserId);

        given(noteRepository.save(any(SalesNote.class))).willReturn(note);
        given(noteRepository.countByClientId(1L)).willReturn(3L);

        // When
        // 실제 서비스 로직에서도 내부적으로 세션이나 SecurityContext에서 ID를 꺼내어 toEntity에 넘길 것입니다.
        noteService.createNote(dto);

        // Then
        verify(noteRepository, times(1)).save(any(SalesNote.class));
        verify(briefingService, times(1)).refreshBriefing(1L);
    }
}