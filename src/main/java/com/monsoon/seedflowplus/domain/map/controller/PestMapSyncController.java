package com.monsoon.seedflowplus.domain.map.controller;

import com.monsoon.seedflowplus.domain.map.service.NcpmsDataSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/map/sync")
@RequiredArgsConstructor
public class PestMapSyncController {

    private final NcpmsDataSyncService ncpmsDataSyncService;

    @PostMapping
    public ResponseEntity<Map<String, String>> triggerDataSync(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "2025") String year,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String month,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String day
    ) {
        // 값 정규화 및 기본값 적용 (기본값: 2025년 7월 16일)
        String finalMonth = (month == null || month.isBlank()) ? "7" : month;
        String finalDay = (day == null || day.isBlank()) ? "16" : day;

        ncpmsDataSyncService.syncPestForecastData(year, finalMonth, finalDay);

        String dateMessage = formatSyncDate(year, finalMonth, finalDay);
        return ResponseEntity.accepted().body(
                Map.of("status", "processing", "message", String.format("%s 기준 데이터 동기화가 백그라운드에서 시작되었습니다.", dateMessage))
        );
    }

    /**
     * 동기화 날짜 메시지를 포맷팅하는 헬퍼 메서드
     */
    private String formatSyncDate(String year, String month, String day) {
        return String.format("%s년 %s월 %s일", year, month, day);
    }
}