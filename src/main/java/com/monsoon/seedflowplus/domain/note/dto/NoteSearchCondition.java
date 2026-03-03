package com.monsoon.seedflowplus.domain.note.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "영업 활동 기록 검색 필터 조건")
public class NoteSearchCondition {

    @Schema(description = "특정 고객사 ID 필터", example = "10")
    private Long clientId;      // 특정 고객사 필터

    @Schema(description = "특정 계약 ID 필터", example = "CONT-2024-001")
    private String contractId;  // 특정 계약 필터

    @Schema(description = "검색 키워드 (고객명, 내용, 요약 등)", example = "가격 협상")
    private String keyword;     // 고객명, 내용, 요약 키워드 검색

    @Schema(description = "검색 시작일", example = "2024-01-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom; // 검색 시작일

    @Schema(description = "검색 종료일", example = "2024-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;   // 검색 종료일

    @Schema(description = "정렬 기준 (desc: 최신순, asc: 오래된순)", example = "desc")
    private String sort;        // 정렬 기준 ("desc" - 최신순, "asc" - 오래된순)
}