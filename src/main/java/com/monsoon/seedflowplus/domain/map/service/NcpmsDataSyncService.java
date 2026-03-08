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
            if (allItems.isEmpty()) {
                log.warn("NCPMS API로부터 수집된 작물 목록이 없습니다.");
                return;
            }

            // [추가] 2. 기존 데이터 한 번만 삭제 (배치 저장 시작 전)
            transactionTemplate.executeWithoutResult(status -> {
                pestForecastRepository.deleteAllInBatch();
            });
            log.info("기존 예찰 데이터를 모두 삭제하고 새로운 동기화를 시작합니다.");

            int totalItems = allItems.size();
            int currentItemIndex = 0;
            int totalSavedCount = 0;

            for (NcpmsListDto item : allItems) {
                currentItemIndex++;
                try {
                    log.info("[{}/{}] 아이템 처리 시작: insectKey={}, cropName={}", 
                             currentItemIndex, totalItems, item.getInsectKey(), item.getKncrNm());
                    
                    // 해당 작물에 대한 데이터 수집용 리스트
                    List<PestForecast> currentItemForecasts = new ArrayList<>();
                    processMainItem(item, currentItemForecasts);
                    
                    // [변경] 3. 작물 1개 처리가 끝날 때마다 즉시 DB 저장
                    if (!currentItemForecasts.isEmpty()) {
                        final int savedCountInItem = currentItemForecasts.size();
                        transactionTemplate.executeWithoutResult(status -> {
                            pestForecastRepository.saveAll(currentItemForecasts);
                        });
                        totalSavedCount += savedCountInItem;
                        log.info(">> [{}/{}] {} 처리 완료: {}건 저장 (현재 누적: {}건)", 
                                 currentItemIndex, totalItems, item.getKncrNm(), savedCountInItem, totalSavedCount);
                    } else {
                        log.info(">> [{}/{}] {} : 수집된 유효 데이터가 없습니다.", 
                                 currentItemIndex, totalItems, item.getKncrNm());
                    }

                    // Rate limit: NCPMS 서버 보호 및 차단 방지
                    Thread.sleep(150);
                } catch (Exception e) {
                    log.error("아이템 처리 실패 (건너뜀) - insectKey: {}, 사유: {}", item.getInsectKey(), e.getMessage());
                }
            }

            log.info("NCPMS 동기화 최종 완료! 총 {}개 작물군에 대해 {}건의 데이터가 저장되었습니다.", totalItems, totalSavedCount);

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
        log.info("sidoList size for insectKey {}: {}", insectKey, sidoList.size());

        for (NcpmsSidoDto sido : sidoList) {
            Thread.sleep(50);
            List<NcpmsSigunguDto> sigunguList = ncpmsApiClient.fetchSigungu(insectKey, sido.getSidoCode());
            log.info("sigunguList size for sido {}: {}", sido.getSidoNm(), sigunguList.size());
            List<PestForecast> entities = convertToEntityList(item.getKncrNm(), sigunguList);
            log.info("entities created from sigunguList: {}", entities.size());
            accumulated.addAll(entities);
        }
    }

    private List<PestForecast> convertToEntityList(String cropName, List<NcpmsSigunguDto> sigunguDataList) {
        List<PestForecast> entities = new ArrayList<>();
        String cropCode = mapCropNameToCode(cropName);

        for (NcpmsSigunguDto dto : sigunguDataList) {
            String pestCode = mapPestNameToCode(dto.getDbyhsNm());
            String severity = convertValueToSeverity(dto.getInqireValue());

            log.info("Mapping: cropName={}, dbyhsNm={}, cropCode={}, pestCode={}", cropName, dto.getDbyhsNm(), cropCode, pestCode);

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
        if (name.contains("파")) return "onion"; 
        if (name.contains("상추")) return "lettuce";
        if (name.contains("감자")) return "potato";
        if (name.contains("토마토")) return "tomato";
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
        if (name.contains("시들음병")) return "P06";
        if (name.contains("잎마름병")) return "P07";
        if (name.contains("진딧물")) return "P08";
        if (name.contains("나방")) return "P09";
        if (name.contains("응애")) return "P10";
        if (name.contains("균핵병")) return "P11";
        return "UNKNOWN";
    }
}
