package com.monsoon.seedflowplus.domain.map.service;

import com.monsoon.seedflowplus.domain.map.dto.response.NcpmsApiResponse.*;
import com.monsoon.seedflowplus.domain.map.entity.PestForecast;
import com.monsoon.seedflowplus.domain.map.repository.PestForecastRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NcpmsDataSyncService {

    private final PestForecastRepository pestForecastRepository;
    private final WebClient webClient = WebClient.create("http://ncpms.rda.go.kr/npmsAPI/service");

    // 작물 및 병해충 매핑 데이터 (향후 DB나 외부 설정으로 분리 가능)
    private static final Map<String, String> CROP_NAME_TO_CODE = Map.of(
            "배추", "cabbage",
            "가을배추", "cabbage",
            "고추", "pepper",
            "풋고추", "pepper",
            "마늘", "garlic",
            "양파", "onion",
            "무", "radish"
    );

    private static final Map<String, String> PEST_NAME_TO_CODE = Map.of(
            "노균병", "P01",
            "무름병", "P02",
            "탄저병", "P03",
            "뿌리혹병", "P04"
    );

    @Value("${ncpms.api.key}")
    private String apiKey;

    @Async("briefingTaskExecutor")
    @Transactional
    public void syncPestForecastData() {
        log.info("NCPMS 수동 동기화 시작 (백그라운드 스레드 동작)");

        try {
            pestForecastRepository.deleteAllInBatch();

            List<NcpmsListDto> listResponse = fetchList();
            List<PestForecast> newForecasts = new ArrayList<>();

            if (listResponse != null) {
                for (NcpmsListDto listDto : listResponse) {
                    String insectKey = listDto.getInsectKey();

                    List<NcpmsSidoDto> sidoResponse = fetchSidoDetails(insectKey);
                    if (sidoResponse != null) {
                        for (NcpmsSidoDto sidoDto : sidoResponse) {
                            List<NcpmsSigunguDto> sigunguResponse = fetchSigunguDetails(insectKey, sidoDto.getSidoCode());
                            if (sigunguResponse != null) {
                                newForecasts.addAll(convertToEntityList(listDto.getKncrNm(), sigunguResponse));
                            }
                        }
                    }
                }
            }

            pestForecastRepository.saveAll(newForecasts);
            log.info("NCPMS 데이터 동기화 완료. 총 {}건 저장", newForecasts.size());

        } catch (Exception e) {
            log.error("NCPMS 데이터 동기화 실패: {}", e.getMessage(), e);
        }
    }

    private List<PestForecast> convertToEntityList(String cropName, List<NcpmsSigunguDto> sigunguDataList) {
        List<PestForecast> entities = new ArrayList<>();
        for (NcpmsSigunguDto dto : sigunguDataList) {
            String severity = convertValueToSeverity(dto.getInqireValue());
            String cropCode = mapCropNameToCode(cropName);
            String pestCode = mapPestNameToCode(dto.getDbyhsNm());

            entities.add(PestForecast.builder()
                    .areaName(dto.getSigunguNm())
                    .cropCode(cropCode)
                    .pestCode(pestCode)
                    .severity(severity)
                    .build());
        }
        return entities;
    }

    private List<NcpmsListDto> fetchList() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("apiKey", apiKey)
                        .queryParam("serviceCode", "SVC51")
                        .queryParam("serviceType", "AA003")
                        .queryParam("displayCount", 50)
                        .build())
                .retrieve()
                .bodyToFlux(NcpmsListDto.class)
                .collectList()
                .block();
    }

    private List<NcpmsSidoDto> fetchSidoDetails(String insectKey) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("apiKey", apiKey)
                        .queryParam("serviceCode", "SVC52")
                        .queryParam("serviceType", "AA003")
                        .queryParam("insectKey", insectKey)
                        .build())
                .retrieve()
                .bodyToFlux(NcpmsSidoDto.class)
                .collectList()
                .block();
    }

    private List<NcpmsSigunguDto> fetchSigunguDetails(String insectKey, String sidoCode) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("apiKey", apiKey)
                        .queryParam("serviceCode", "SVC53")
                        .queryParam("serviceType", "AA003")
                        .queryParam("insectKey", insectKey)
                        .queryParam("sidoCode", sidoCode)
                        .build())
                .retrieve()
                .bodyToFlux(NcpmsSigunguDto.class)
                .collectList()
                .block();
    }

    private String convertValueToSeverity(Integer value) {
        if (value == null) return "보통";
        if (value >= 80) return "심각";
        if (value >= 50) return "경고";
        if (value >= 20) return "주의";
        return "보통";
    }

    /**
     * 작물명을 시스템 코드로 변환합니다.
     */
    String mapCropNameToCode(String cropName) {
        if (cropName == null || cropName.isBlank()) {
            return "UNKNOWN";
        }
        String normalized = cropName.trim();
        return CROP_NAME_TO_CODE.getOrDefault(normalized, "UNKNOWN");
    }

    /**
     * 병해충명을 시스템 코드로 변환합니다.
     */
    String mapPestNameToCode(String pestName) {
        if (pestName == null || pestName.isBlank()) {
            return "UNKNOWN";
        }
        String normalized = pestName.trim();
        return PEST_NAME_TO_CODE.getOrDefault(normalized, "UNKNOWN");
    }
}