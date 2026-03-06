package com.monsoon.seedflowplus.domain.map.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.monsoon.seedflowplus.domain.map.dto.response.NcpmsApiResponse.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NcpmsApiClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final XmlMapper xmlMapper = new XmlMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ncpms.api.key}")
    private String apiKey;

    @Value("${ncpms.api.url}")
    private String baseUrl;

    private static final int DISPLAY_COUNT = 50;

    /**
     * 페이지네이션을 처리하여 연도의 모든 예찰 목록을 수집합니다.
     */
    public List<NcpmsListDto> fetchAllList(String year) {
        List<NcpmsListDto> result = new ArrayList<>();
        int startPoint = 1;

        while (true) {
            URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("apiKey", apiKey)
                    .queryParam("serviceCode", "SVC51")
                    .queryParam("serviceType", "AA003")
                    .queryParam("displayCount", DISPLAY_COUNT)
                    .queryParam("startPoint", startPoint)
                    .queryParam("searchExaminYear", year) // 실제 파라미터명 수정
                    .build().toUri();

            NcpmsListResponse response = executeWithRetry(uri, NcpmsListResponse.class);

            if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
                break;
            }

            List<NcpmsListDto> items = response.getItems();
            result.addAll(items);

            log.info("NCPMS 목록 수집 중... (startPoint: {}, 추가: {}건)", startPoint, items.size());

            if (items.size() < DISPLAY_COUNT) {
                break;
            }
            startPoint += DISPLAY_COUNT;
        }

        log.info("NCPMS LIST 전체 수집 완료: {}건", result.size());
        return result;
    }

    public List<NcpmsSidoDto> fetchSido(String insectKey) {
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("apiKey", apiKey)
                .queryParam("serviceCode", "SVC52")
                .queryParam("serviceType", "AA003")
                .queryParam("insectKey", insectKey)
                .build().toUri();

        NcpmsSidoResponse response = executeWithRetry(uri, NcpmsSidoResponse.class);
        return response != null ? response.getItems() : new ArrayList<>();
    }

    public List<NcpmsSigunguDto> fetchSigungu(String insectKey, String sidoCode) {
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("apiKey", apiKey)
                .queryParam("serviceCode", "SVC53")
                .queryParam("serviceType", "AA003")
                .queryParam("insectKey", insectKey)
                .queryParam("sidoCode", sidoCode)
                .build().toUri();

        NcpmsSigunguResponse response = executeWithRetry(uri, NcpmsSigunguResponse.class);
        return response != null ? response.getItems() : new ArrayList<>();
    }

    /**
     * 최대 3회 재시도를 포함한 API 실행 및 파싱
     */
    private <T> T executeWithRetry(URI uri, Class<T> clazz) {
        int retry = 0;
        while (retry < 3) {
            try {
                String content = restTemplate.getForObject(uri, String.class);
                if (content == null || content.isBlank()) return null;

                String trimmed = content.trim();
                if (trimmed.startsWith("{")) {
                    JsonNode root = objectMapper.readTree(trimmed);
                    JsonNode serviceNode = root.get("service");
                    return serviceNode != null ? objectMapper.treeToValue(serviceNode, clazz) : objectMapper.readValue(trimmed, clazz);
                } else {
                    return xmlMapper.readValue(trimmed, clazz);
                }
            } catch (Exception e) {
                retry++;
                log.warn("NCPMS API 재시도 ({}/3) - URI: {}, 사유: {}", retry, uri, e.getMessage());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {}
            }
        }
        log.error("NCPMS API 최종 실패 - URI: {}", uri);
        return null;
    }
}
