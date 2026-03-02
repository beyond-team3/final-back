package com.monsoon.seedflowplus.domain.map.service;

import com.monsoon.seedflowplus.domain.map.dto.response.NcpmsApiResponse.*;
import com.monsoon.seedflowplus.domain.map.entity.PestForecast;
import com.monsoon.seedflowplus.domain.map.repository.PestForecastRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NcpmsDataSyncService {

    private final PestForecastRepository pestForecastRepository;
    private final TransactionTemplate transactionTemplate;
    private final WebClient ncpmsWebClient;

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
    public void syncPestForecastData() {
        log.info("NCPMS 수동 동기화 시작 (백그라운드 스레드 동작)");

        try {
            // 1. 데이터 먼저 수집 (트랜잭션 밖에서 네트워크 I/O 수행)
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

            // 2. 수집된 데이터가 있을 경우에만 트랜잭션 내에서 원자적 교체 수행
            if (!newForecasts.isEmpty()) {
                transactionTemplate.executeWithoutResult(status -> {
                    pestForecastRepository.deleteAllInBatch();
                    pestForecastRepository.saveAll(newForecasts);
                });
                log.info("NCPMS 데이터 동기화 완료. 총 {}건 저장", newForecasts.size());
            } else {
                log.warn("NCPMS로부터 수집된 데이터가 없어 동기화를 중단합니다.");
            }

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
        return ncpmsWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("apiKey", apiKey)
                        .queryParam("serviceCode", "SVC51")
                        .queryParam("serviceType", "AA003")
                        .queryParam("displayCount", 50)
                        .build())
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("NCPMS API 에러: " + response.statusCode() + ", " + body))))
                .bodyToFlux(NcpmsListDto.class)
                .collectList()
                .timeout(Duration.ofSeconds(10))
                .onErrorMap(e -> new RuntimeException("NCPMS 목록 조회 중 오류 발생", e))
                .block();
    }

    private List<NcpmsSidoDto> fetchSidoDetails(String insectKey) {
        return ncpmsWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("apiKey", apiKey)
                        .queryParam("serviceCode", "SVC52")
                        .queryParam("serviceType", "AA003")
                        .queryParam("insectKey", insectKey)
                        .build())
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("NCPMS API 에러: " + response.statusCode() + ", " + body))))
                .bodyToFlux(NcpmsSidoDto.class)
                .collectList()
                .timeout(Duration.ofSeconds(10))
                .onErrorMap(e -> new RuntimeException("NCPMS 시도 상세 조회 중 오류 발생 (insectKey: " + insectKey + ")", e))
                .block();
    }

    private List<NcpmsSigunguDto> fetchSigunguDetails(String insectKey, String sidoCode) {
        return ncpmsWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("apiKey", apiKey)
                        .queryParam("serviceCode", "SVC53")
                        .queryParam("serviceType", "AA003")
                        .queryParam("insectKey", insectKey)
                        .queryParam("sidoCode", sidoCode)
                        .build())
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("NCPMS API 에러: " + response.statusCode() + ", " + body))))
                .bodyToFlux(NcpmsSigunguDto.class)
                .collectList()
                .timeout(Duration.ofSeconds(10))
                .onErrorMap(e -> new RuntimeException("NCPMS 시군구 상세 조회 중 오류 발생 (sidoCode: " + sidoCode + ")", e))
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