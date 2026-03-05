package com.monsoon.seedflowplus.infra.kakao;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.infra.kakao.dto.KakaoGeoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final WebClient.Builder webClientBuilder;

    @Value("${kakao.api.local-url}")
    private String apiUrl;

    @Value("${kakao.api.key}")
    private String apiKey;

    public GeoPoint getGeoPoint(String address) {
        if (address == null || address.isBlank()) {
            throw new CoreException(ErrorType.INVALID_INPUT_VALUE);
        }

        log.info("Kakao Geocoding 요청 주소: {}", address);

        KakaoGeoResponse response = webClientBuilder.build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .fromHttpUrl(apiUrl) // path() 대신 fromHttpUrl() 사용
                        .queryParam("query", address)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
                .retrieve()
                .bodyToMono(KakaoGeoResponse.class)
                .block();

        if (response == null || response.getDocuments() == null || response.getDocuments().isEmpty()) {
            log.warn("주소 검색 결과가 없습니다: {}", address);
            // 주소 검색 실패 시 예외 처리 (GlobalException 연동을 위해 CoreException 사용)
            throw new CoreException(ErrorType.ADDRESS_NOT_FOUND);
        }

        KakaoGeoResponse.Document doc = response.getDocuments().get(0);
        double lng = Double.parseDouble(doc.getLongitude());
        double lat = Double.parseDouble(doc.getLatitude());

        return new GeoPoint(lat, lng);
    }

    /**
     * Client 엔티티의 주소를 기반으로 좌표를 변환하여 엔티티 필드를 직접 채웁니다.
     */
    public void fillCoordinates(com.monsoon.seedflowplus.domain.account.entity.Client client) {
        if (client == null || client.getAddress() == null) return;
        GeoPoint point = getGeoPoint(client.getAddress());
        client.updateCoordinates(point.latitude(), point.longitude());
    }

    public record GeoPoint(double latitude, double longitude) {}
}
