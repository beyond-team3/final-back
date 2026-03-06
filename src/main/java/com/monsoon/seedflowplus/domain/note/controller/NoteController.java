package com.monsoon.seedflowplus.domain.note.controller;

import com.monsoon.seedflowplus.core.common.support.response.ApiResult;
import com.monsoon.seedflowplus.domain.note.dto.request.NoteRequestDto;
import com.monsoon.seedflowplus.domain.note.dto.NoteSearchCondition;
import com.monsoon.seedflowplus.domain.note.dto.response.NoteResponseDto;
import com.monsoon.seedflowplus.domain.note.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Sales Note", description = "영업 활동 기록 관리 API")
@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    /**
     * 1. 영업 노트 목록 조회 및 검색
     * NoteSearchView.vue의 필터링 및 검색 기능을 담당합니다.
     */
    @Operation(summary = "영업 활동 기록 목록 조회 및 검색", description = "필터 조건에 따른 영업 노트 목록을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<ApiResult<List<NoteResponseDto>>> getNotes(@Parameter(description = "검색 및 필터 조건") NoteSearchCondition condition) {
        List<NoteResponseDto> notes = noteService.searchNotes(condition).stream()
                .map(NoteResponseDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResult.success(notes));
    }

    /**
     * 2. 영업 노트 상세 저장 및 AI 요약 분석
     * NoteView.vue에서 '저장' 버튼 클릭 시 호출됩니다.
     */
    @Operation(summary = "영업 활동 기록 저장", description = "새로운 영업 활동을 기록하고 AI 요약 분석을 함께 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "저장 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    @PostMapping
    public ResponseEntity<ApiResult<NoteResponseDto>> createNote(@Valid @RequestBody NoteRequestDto dto) {
        NoteResponseDto createdNote = NoteResponseDto.from(noteService.createNote(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.success(createdNote));
    }

    /**
     * 3. 영업 활동 기록 수정 및 AI 재분석
     * NoteView.vue에서 '수정' 버튼 클릭 시 호출됩니다.
     */
    @Operation(summary = "영업 활동 기록 수정", description = "기존 영업 노트를 수정하고 AI 재분석을 수행합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터",
                    content = @Content(schema = @Schema(implementation = ApiResult.class))),
            @ApiResponse(responseCode = "404", description = "해당 ID의 노트를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResult<NoteResponseDto>> updateNote(
            @Parameter(description = "영업 노트 ID") @PathVariable Long id,
            @Valid @RequestBody NoteRequestDto dto) {
        NoteResponseDto updatedNote = NoteResponseDto.from(noteService.updateNote(id, dto));
        return ResponseEntity.ok(ApiResult.success(updatedNote));
    }

    /**
     * 4. 영업 활동 기록 삭제
     * NoteSearchView.vue에서 '삭제' 버튼 클릭 시 호출됩니다.
     */
    @Operation(summary = "영업 활동 기록 삭제", description = "지정된 ID의 영업 활동 기록을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "해당 ID의 노트를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResult.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@Parameter(description = "영업 노트 ID") @PathVariable Long id) {
        noteService.deleteNote(id);
        return ResponseEntity.noContent().build();
    }
}
