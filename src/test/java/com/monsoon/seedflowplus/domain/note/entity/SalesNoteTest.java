package com.monsoon.seedflowplus.domain.note.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class SalesNoteTest {

    @Test
    @DisplayName("노트 내용 수정 시 isEdited 플래그가 true로 변경되어야 한다")
    void updateNote_SetsIsEditedToTrue() {
        // Given
        SalesNote note = SalesNote.builder()
                .content("원본 내용")
                .build();

        // When
        note.updateNote("수정된 내용", "계약A", LocalDate.now(), List.of("요약1"));

        // Then
        assertThat(note.getContent()).isEqualTo("수정된 내용");
        assertThat(note.isEdited()).isTrue();
    }
}