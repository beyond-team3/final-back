package com.monsoon.seedflowplus.domain.map.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.monsoon.seedflowplus.domain.map.dto.response.NcpmsApiResponse.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NcpmsApiClient {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final XmlMapper xmlMapper = new XmlMapper();
    private RestTemplate restTemplate;

    @Value("${ncpms.api.key:}")
    private String apiKey;

    @Value("${ncpms.api.url:}")
    private String baseUrl;

    private static final int DISPLAY_COUNT = 50;
    private static final int TIMEOUT_MS = 5000;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT_MS);
        factory.setReadTimeout(TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
    }

    private boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && baseUrl != null && !baseUrl.isBlank();
    }

    /**
     * 로그 출력용으로 URI에서 apiKey를 마스킹합니다.
     */
    private String sanitizeUri(URI uri) {
        if (uri == null) return "null";
        return UriComponentsBuilder.fromUri(uri)
                .replaceQueryParam("apiKey", "****")
                .build()
                .toUriString();
    }

    /**
     * 특정 날짜(연, 월, 일)의 모든 예찰 목록을 수집합니다.
     */
    public List<NcpmsListDto> fetchAllList(String year, String month, String day) {
        if (!isConfigured()) {
            log.warn("NCPMS API가 설정되지 않았습니다. (apiKey 또는 baseUrl 누락)");
            return new ArrayList<>();
        }
        List<NcpmsListDto> result = new ArrayList<>();
        int startPoint = 1;

        while (true) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("apiKey", apiKey)
                    .queryParam("serviceCode", "SVC51")
                    .queryParam("serviceType", "AA003")
                    .queryParam("displayCount", DISPLAY_COUNT)
                    .queryParam("startPoint", startPoint)
                    .queryParam("searchExaminYear", year);
            
            if (month != null && !month.isBlank()) {
                builder.queryParam("searchExaminMonth", month);
            }
            if (day != null && !day.isBlank()) {
                builder.queryParam("searchExaminDe", day);
            }

            URI uri = builder.build().toUri();

            NcpmsListResponse response = executeWithRetry(uri, NcpmsListResponse.class);

            if (response == null) {
                throw new RuntimeException("NCPMS 목록 API 호출 최종 실패 - URI: " + sanitizeUri(uri));
            }

            if (response.getItems() == null || response.getItems().isEmpty()) {
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
        if (!isConfigured()) {
            return new ArrayList<>();
        }
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("apiKey", apiKey)
                .queryParam("serviceCode", "SVC52")
                .queryParam("serviceType", "AA003")
                .queryParam("insectKey", insectKey)
                .build().toUri();

        NcpmsSidoResponse response = executeWithRetry(uri, NcpmsSidoResponse.class);
        if (response == null) {
            throw new RuntimeException("NCPMS Sido API 호출 최종 실패 - URI: " + sanitizeUri(uri));
        }
        return response.getItems() != null ? response.getItems() : new ArrayList<>();
    }

    public List<NcpmsSigunguDto> fetchSigungu(String insectKey, String sidoCode) {
        if (!isConfigured()) {
            return new ArrayList<>();
        }
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("apiKey", apiKey)
                .queryParam("serviceCode", "SVC53")
                .queryParam("serviceType", "AA003")
                .queryParam("insectKey", insectKey)
                .queryParam("sidoCode", sidoCode)
                .build().toUri();

        NcpmsSigunguResponse response = executeWithRetry(uri, NcpmsSigunguResponse.class);
        if (response == null) {
            throw new RuntimeException("NCPMS Sigungu API 호출 최종 실패 - URI: " + sanitizeUri(uri));
        }
        return response.getItems() != null ? response.getItems() : new ArrayList<>();
    }

    /**
     * 최대 3회 재시도를 포함한 API 실행 및 파싱
     */
    private <T> T executeWithRetry(URI uri, Class<T> clazz) {
        int retry = 0;
        String sanitizedUri = sanitizeUri(uri);
        while (retry < 3) {
            try {
                String content = restTemplate.getForObject(uri, String.class);
                if (content == null || content.isBlank()) return null;

                String trimmed = content.trim();
                if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                    JsonNode root = objectMapper.readTree(trimmed);
                    
                    // Handle wrapped response: { "service": { ... } }
                    if (root.has("service")) {
                        return objectMapper.treeToValue(root.get("service"), clazz);
                    }
                    
                    // Handle direct array response or already correct structure
                    return objectMapper.treeToValue(root, clazz);
                } else if (trimmed.startsWith("<")) {
                    return xmlMapper.readValue(trimmed, clazz);
                } else {
                    log.warn("Unknown response format: {}", trimmed.substring(0, Math.min(trimmed.length(), 50)));
                    return null;
                }
            } catch (Exception e) {
                retry++;
                log.warn("NCPMS API 재시도 ({}/3) - URI: {}, 사유: {}", retry, sanitizedUri, e.getMessage());
                try {
                    Thread.sleep(1000); // 1초 대기
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.error("NCPMS API 최종 실패 - URI: {}", sanitizedUri);
        return null;
    }
}
