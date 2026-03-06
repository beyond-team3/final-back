package com.monsoon.seedflowplus.domain.map.service;

import com.monsoon.seedflowplus.domain.map.dto.response.NcpmsApiResponse.*;
import com.monsoon.seedflowplus.domain.map.entity.PestForecast;
import com.monsoon.seedflowplus.domain.map.repository.PestForecastRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NcpmsDataSyncService {

    private final PestForecastRepository pestForecastRepository;
    private final TransactionTemplate transactionTemplate;
    private final NcpmsApiClient ncpmsApiClient;

    @Async("briefingTaskExecutor")
    public void syncPestForecastData() {
        log.info("NCPMS 동기화 시작 (2025년 기준)");

        try {
            // 1. 모든 목록 수집 (페이지네이션 포함)
            List<NcpmsListDto> allItems = ncpmsApiClient.fetchAllList("2025");
            List<PestForecast> accumulatedForecasts = new ArrayList<>();

            for (NcpmsListDto item : allItems) {
                try {
                    processMainItem(item, accumulatedForecasts);
                    // Rate limit: NCPMS 서버 보호 및 차단 방지
                    Thread.sleep(150);
                } catch (Exception e) {
                    log.error("아이템 처리 실패 (건너뜀) - insectKey: {}, 사유: {}", item.getInsectKey(), e.getMessage());
                }
            }

            // 2. 수집된 데이터 영속화
            if (!accumulatedForecasts.isEmpty()) {
                transactionTemplate.executeWithoutResult(status -> {
                    pestForecastRepository.deleteAllInBatch();
                    pestForecastRepository.saveAll(accumulatedForecasts);
                });
                log.info("NCPMS 동기화 최종 완료! 총 {}건 저장되었습니다.", accumulatedForecasts.size());
            } else {
                log.warn("수집된 유효 데이터가 없어 업데이트를 수행하지 않았습니다.");
            }

        } catch (Exception e) {
            log.error("NCPMS 동기화 프로세스 전체 실패", e);
        }
    }

    /**
     * 개별 예찰 아이템에 대해 시도 -> 시군구 상세 조회를 수행하고 엔티티로 변환합니다.
     */
    private void processMainItem(NcpmsListDto item, List<PestForecast> accumulated) throws InterruptedException {
        String insectKey = item.getInsectKey();
        List<NcpmsSidoDto> sidoList = ncpmsApiClient.fetchSido(insectKey);

        for (NcpmsSidoDto sido : sidoList) {
            Thread.sleep(50);
            List<NcpmsSigunguDto> sigunguList = ncpmsApiClient.fetchSigungu(insectKey, sido.getSidoCode());
            accumulated.addAll(convertToEntityList(item.getKncrNm(), sigunguList));
        }
    }

    private List<PestForecast> convertToEntityList(String cropName, List<NcpmsSigunguDto> sigunguDataList) {
        List<PestForecast> entities = new ArrayList<>();
        String cropCode = mapCropNameToCode(cropName);

        for (NcpmsSigunguDto dto : sigunguDataList) {
            String pestCode = mapPestNameToCode(dto.getDbyhsNm());
            String severity = convertValueToSeverity(dto.getInqireValue());

            // 유효한 매핑 결과가 있는 경우에만 수집
            if (!"UNKNOWN".equals(cropCode) && !"UNKNOWN".equals(pestCode)) {
                entities.add(PestForecast.builder()
                        .areaName(dto.getSigunguNm())
                        .cropCode(cropCode)
                        .pestCode(pestCode)
                        .severity(severity)
                        .build());
            }
        }
        return entities;
    }

    private String convertValueToSeverity(Integer value) {
        if (value == null) return "보통";
        if (value >= 80) return "심각";
        if (value >= 50) return "경고";
        if (value >= 20) return "주의";
        return "보통";
    }

    /**
     * 안정적인 작물 매핑 (부분 일치 허용)
     */
    private String mapCropNameToCode(String name) {
        if (name == null) return "UNKNOWN";
        if (name.contains("고추")) return "pepper";
        if (name.contains("배추")) return "cabbage";
        if (name.contains("마늘")) return "garlic";
        if (name.contains("양파")) return "onion";
        if (name.contains("무")) return "radish";
        return "UNKNOWN";
    }

    /**
     * 안정적인 병해충 매핑 (부분 일치 허용)
     */
    private String mapPestNameToCode(String name) {
        if (name == null) return "UNKNOWN";
        if (name.contains("노균병")) return "P01";
        if (name.contains("무름병")) return "P02";
        if (name.contains("탄저병")) return "P03";
        if (name.contains("뿌리혹병")) return "P04";
        if (name.contains("역병")) return "P05";
        return "UNKNOWN";
    }
}
