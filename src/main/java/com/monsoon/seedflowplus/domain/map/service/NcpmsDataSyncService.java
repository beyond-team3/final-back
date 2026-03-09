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
    public void syncPestForecastData(String year, String month, String day) {
        log.info("NCPMS 동기화 시작 (기준 날짜: {}년 {}월 {}일)", year, month, day);

        try {
            // 1. 특정 날짜 목록 수집 (페이지네이션 포함)
            List<NcpmsListDto> allItems = ncpmsApiClient.fetchAllList(year, month, day);
            if (allItems.isEmpty()) {
                log.warn("{}년 {}월 {}일 기준 NCPMS API로부터 수집된 작물 목록이 없습니다.", year, month, day);
                return;
            }

            int totalItems = allItems.size();
            int currentItemIndex = 0;
            List<PestForecast> allForecastsToSave = new ArrayList<>();

            // 2. 모든 데이터 먼저 수집 (Prepare)
            for (NcpmsListDto item : allItems) {
                currentItemIndex++;
                try {
                    String cropName = item.getKncrNm();
                    String cropCode = mapCropNameToCode(cropName);

                    if ("UNKNOWN".equals(cropCode)) {
                        continue;
                    }

                    log.info("[{}/{}] 데이터 수집 시작: insectKey={}, cropName={}", 
                             currentItemIndex, totalItems, item.getInsectKey(), cropName);
                    
                    List<PestForecast> currentItemForecasts = new ArrayList<>();
                    processMainItem(item, currentItemForecasts);
                    
                    if (!currentItemForecasts.isEmpty()) {
                        // 중복 제거 (지역+작물+병해충 별로 가장 높은 위험도 하나만 남김)
                        List<PestForecast> distinctForecasts = new ArrayList<>(
                            currentItemForecasts.stream()
                                .collect(java.util.stream.Collectors.toMap(
                                    f -> f.getSidoCode() + "|" + f.getSigunguCode() + "|" + f.getCropCode() + "|" + f.getPestCode(),
                                    f -> f,
                                    (existing, replacement) -> getMoreSevere(existing, replacement)
                                ))
                                .values()
                        );
                        allForecastsToSave.addAll(distinctForecasts);
                    }

                    // Rate limit: NCPMS 서버 보호 및 차단 방지
                    Thread.sleep(150);
                } catch (Exception e) {
                    log.error("아이템 수집 실패 (건너뜀) - insectKey: {}, 사유: {}", item.getInsectKey(), e.getMessage());
                }
            }

            // 3. 단일 트랜잭션에서 기존 데이터 삭제 후 새 데이터 저장 (Swap)
            if (!allForecastsToSave.isEmpty()) {
                transactionTemplate.executeWithoutResult(status -> {
                    pestForecastRepository.deleteAllInBatch();
                    pestForecastRepository.saveAll(allForecastsToSave);
                });
                log.info("NCPMS 동기화 최종 완료! 총 {}건의 데이터가 원자적으로 교체되었습니다.", allForecastsToSave.size());
            } else {
                log.warn("NCPMS 동기화 중단: 수집된 유효 데이터가 없습니다. 기존 데이터를 유지합니다.");
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
        int totalSido = sidoList.size();
        log.info(">> {} 시도 목록 수집 완료 (총 {}개 시도)", item.getKncrNm(), totalSido);

        for (int i = 0; i < totalSido; i++) {
            NcpmsSidoDto sido = sidoList.get(i);
            Thread.sleep(100); // API 서버 부하 방지용 대기
            
            List<NcpmsSigunguDto> sigunguList = ncpmsApiClient.fetchSigungu(insectKey, sido.getSidoCode());
            if (sigunguList != null && !sigunguList.isEmpty()) {
                List<PestForecast> entities = convertToEntityList(item.getKncrNm(), sigunguList);
                accumulated.addAll(entities);
                log.info("   - [{}/{}] {} 처리: {}건 수집", (i + 1), totalSido, sido.getSidoNm(), entities.size());
            } else {
                log.info("   - [{}/{}] {} 처리: 데이터 없음", (i + 1), totalSido, sido.getSidoNm());
            }
        }
    }

    private List<PestForecast> convertToEntityList(String cropName, List<NcpmsSigunguDto> sigunguDataList) {
        List<PestForecast> entities = new ArrayList<>();
        if (sigunguDataList == null) return entities;
        
        String cropCode = mapCropNameToCode(cropName);

        for (NcpmsSigunguDto dto : sigunguDataList) {
            if (dto == null) continue;
            
            String pestCode = mapPestNameToCode(dto.getDbyhsNm());
            String severity = convertValueToSeverity(dto.getInqireValue());

            // 유효한 매핑 결과가 있는 경우에만 수집
            if (!"UNKNOWN".equals(cropCode) && !"UNKNOWN".equals(pestCode)) {
                entities.add(PestForecast.builder()
                        .areaName(dto.getSigunguNm() != null ? dto.getSigunguNm() : "Unknown")
                        .sidoCode(dto.getSidoCode())
                        .sigunguCode(dto.getSigunguCode())
                        .cropCode(cropCode)
                        .pestCode(pestCode)
                        .severity(severity)
                        .build());
            }
        }
        return entities;
    }

    private String convertValueToSeverity(Double value) {
        if (value == null || value <= 0) return "보통";

        // 0~5 범위의 소수점 데이터를 기반으로 한 정교한 위험도 산정
        if (value >= 3.0) return "심각";
        if (value >= 1.5) return "경고";
        if (value >= 0.1) return "주의";
        
        return "보통";
    }

    /**
     * 안정적인 작물 매핑 (고추, 양파만 수집)
     */
    private String mapCropNameToCode(String name) {
        if (name == null) return "UNKNOWN";
        if (name.contains("고추")) return "pepper";
        if (name.contains("양파")) return "onion";
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
        if (name.contains("시들음병") || name.contains("위황병")) return "P24";
        if (name.contains("잎마름병")) return "P07";
        if (name.contains("진딧물")) return "P08";
        if (name.contains("나방")) return "P09";
        if (name.contains("응애")) return "P10";
        if (name.contains("균핵병")) return "P11";
        if (name.contains("바이러스")) return "P21";
        if (name.contains("흰가루병")) return "P22";
        if (name.contains("청고병") || name.contains("풋마름병")) return "P23";
        if (name.contains("깜부기병")) return "P25";
        if (name.contains("불마름병") || name.contains("잎가름병")) return "P26";
        
        return "UNKNOWN";
    }

    /**
     * 두 예찰 데이터 중 더 심각한 단계를 가진 데이터를 반환합니다.
     */
    private PestForecast getMoreSevere(PestForecast f1, PestForecast f2) {
        int r1 = getSeverityRank(f1.getSeverity());
        int r2 = getSeverityRank(f2.getSeverity());
        return r1 >= r2 ? f1 : f2;
    }

    private int getSeverityRank(String severity) {
        if (severity == null) return 0;
        return switch (severity) {
            case "심각" -> 3;
            case "경고" -> 2;
            case "주의" -> 1;
            default -> 0; // 보통
        };
    }
}
