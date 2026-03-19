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
            String currentSidoCode = sido.getSidoCode();

            // 시도 코드가 없는 경우 상세 조회를 진행할 수 없으므로 스킵
            if (currentSidoCode == null || currentSidoCode.isBlank()) {
                log.warn("시도 코드가 누락되었습니다. (시도명: {}). 상세 조회를 건너뜁니다.", sido.getSidoNm());
                continue;
            }

            Thread.sleep(100); // API 서버 부하 방지용 대기
            
            // 시군구 단위 상세 데이터 저장
            List<NcpmsSigunguDto> sigunguList = ncpmsApiClient.fetchSigungu(insectKey, currentSidoCode);
            if (sigunguList != null && !sigunguList.isEmpty()) {
                List<PestForecast> entities = convertToEntityList(item.getKncrNm(), sigunguList, currentSidoCode);
                accumulated.addAll(entities);
                log.info("   - [{}/{}] {} 상세 처리: {}건 수집", (i + 1), totalSido, sido.getSidoNm(), entities.size());
            }
        }
    }

    private List<PestForecast> convertToEntityList(String cropName, List<NcpmsSigunguDto> sigunguDataList, String fallbackSidoCode) {
        List<PestForecast> entities = new ArrayList<>();
        if (sigunguDataList == null) return entities;
        
        for (NcpmsSigunguDto dto : sigunguDataList) {
            if (dto == null) continue;
            
            String finalSidoCode = (dto.getSidoCode() != null && !dto.getSidoCode().isBlank()) 
                                    ? dto.getSidoCode() : fallbackSidoCode;

            PestForecast entity = buildPestForecast(
                cropName, 
                dto.getSigunguNm(), 
                finalSidoCode, 
                dto.getSigunguCode(), 
                dto.getDbyhsNm(), 
                dto.getInqireValue()
            );

            if (entity != null) {
                entities.add(entity);
            }
        }
        return entities;
    }

    private PestForecast buildPestForecast(String cropName, String areaName, String sidoCode, String sigunguCode, String dbyhsNm, Double inqireValue) {
        String cropCode = mapCropNameToCode(cropName);
        String pestCode = mapPestNameToCode(dbyhsNm);
        String severity = convertValueToSeverity(inqireValue);

        // 필수 값 검증 (DB Not Null 제약 조건 준수)
        if (!"UNKNOWN".equals(cropCode) && !"UNKNOWN".equals(pestCode) && 
            sidoCode != null && !sidoCode.isBlank() && 
            sigunguCode != null && !sigunguCode.isBlank()) {
            
            return PestForecast.builder()
                    .areaName(areaName != null ? areaName : "Unknown")
                    .sidoCode(sidoCode)
                    .sigunguCode(sigunguCode)
                    .cropCode(cropCode)
                    .pestCode(pestCode)
                    .severity(severity)
                    .build();
        }
        return null;
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
     * 안정적인 작물 매핑 (고추, 양파, 마늘만 수집)
     */
    private String mapCropNameToCode(String name) {
        if (name == null) return "UNKNOWN";
        if (name.contains("고추")) return "PEPPER";
        if (name.contains("양파")) return "ONION";
        if (name.contains("마늘")) return "GARLIC";
        return "UNKNOWN";
    }

    /**
     * 안정적인 병해충 매핑 (NCPMS 주요 병해충 코드 매핑 보강)
     */
    private String mapPestNameToCode(String name) {
        if (name == null) return "UNKNOWN";
        
        // 공통/주요 병해
        if (name.contains("노균병")) return "P01";
        if (name.contains("무름병")) return "P02";
        if (name.contains("탄저병")) return "P03";
        if (name.contains("뿌리혹병")) return "P04";
        if (name.contains("역병")) return "P05";
//        if (name.contains("덩굴마름병")) return "P06";
        if (name.contains("잎마름병")) return "P07";
        if (name.contains("균핵병")) return "P11";
//        if (name.contains("잿빛곰팡이병")) return "P12";
        if (name.contains("잎집썩음병")) return "P13";
        if (name.contains("바이러스")) return "P21";
//        if (name.contains("흰가루병")) return "P22";
//        if (name.contains("청고병") || name.contains("풋마름병")) return "P23";
        
        // 주요 해충
//        if (name.contains("진딧물")) return "P08";
//        if (name.contains("나방")) return "P09";
//        if (name.contains("응애")) return "P10";
//        if (name.contains("총채벌레")) return "P13";
//        if (name.contains("멸구")) return "P14";
//        if (name.contains("가루이")) return "P15";
//        if (name.contains("굴파리")) return "P16";
//
//        // 기타/특수
//        if (name.contains("깜부기병")) return "P25";
//        if (name.contains("불마름병") || name.contains("잎가름병")) return "P26";
        
        // 매핑되지 않은 경우에도 저장을 위해 기본 코드 부여를 고려할 수 있으나, 
        // 현재 buildPestForecast 로직상 UNKNOWN이면 스킵되므로 최대한 매핑을 늘리는 것이 중요합니다.
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
