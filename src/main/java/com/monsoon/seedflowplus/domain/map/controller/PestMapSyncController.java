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
        ncpmsDataSyncService.syncPestForecastData(year, month, day);
        return ResponseEntity.accepted().body(
                Map.of("status", "processing", "message", String.format("%s년 %s월 %s일 기준 데이터 동기화가 백그라운드에서 시작되었습니다.", year, month, day))
        );
    }
}