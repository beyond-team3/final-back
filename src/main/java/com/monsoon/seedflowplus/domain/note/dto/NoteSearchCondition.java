package com.monsoon.seedflowplus.domain.note.dto;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteSearchCondition {

    private Long clientId;      // 특정 고객사 필터
    private String contractId;  // 특정 계약 필터
    private String keyword;     // 고객명, 내용, 요약 키워드 검색

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom; // 검색 시작일

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;   // 검색 종료일

    private String sort;        // 정렬 기준 ("desc" - 최신순, "asc" - 오래된순)
}