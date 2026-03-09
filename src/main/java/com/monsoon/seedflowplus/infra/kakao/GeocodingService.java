package com.monsoon.seedflowplus.infra.kakao;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.core.common.util.AddressParser;
import com.monsoon.seedflowplus.infra.kakao.dto.KakaoGeoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;
import org.springframework.http.HttpStatusCode;

import java.time.Duration;

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

        // AddressParser를 사용하여 실제 검색 가능한 주소 부분(detail)만 추출
        String searchableAddress = AddressParser.parse(address).detail();
        if (searchableAddress.isBlank()) {
            searchableAddress = address; // 파싱 실패 시 원본 사용 시도
        }

        // API 키 유효성 체크 (설정 오류 시 Fail-Fast)
        if (apiKey == null || apiKey.isBlank() || apiKey.contains("DUMMY")) {
            throw new IllegalStateException("Kakao API Key가 설정되지 않았거나 유효하지 않습니다. application-local.yml 설정을 확인하세요.");
        }

        log.debug("Kakao Geocoding 요청 주소 (원본): {}, (검색용): {}", address, searchableAddress);

        // 가장 안정적인 템플릿 방식으로 호출 (자동 인코딩 지원)
        KakaoGeoResponse response = webClientBuilder.build()
                .get()
                .uri(apiUrl.trim() + "?query={query}", searchableAddress.trim())
                .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey.trim())
                .retrieve()
                .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                        .flatMap(errorBody -> {
                            log.error("Kakao API 에러 응답 바디: {}", errorBody);
                            return Mono.error(new CoreException(ErrorType.ADDRESS_NOT_FOUND));
                        }))
                .bodyToMono(KakaoGeoResponse.class)
                .timeout(Duration.ofSeconds(5)) // 요청 타임아웃 추가
                .block();

        if (response == null || response.getDocuments() == null || response.getDocuments().isEmpty()) {
            log.warn("주소 검색 결과가 없습니다: {}", address);
            // 주소 검색 실패 시 예외 처리 (GlobalException 연동을 위해 CoreException 사용)
            throw new CoreException(ErrorType.ADDRESS_NOT_FOUND);
        }

        KakaoGeoResponse.Document doc = response.getDocuments().get(0);

        try {
            double lng = Double.parseDouble(doc.getLongitude());
            double lat = Double.parseDouble(doc.getLatitude());

            log.info("Geocoding 결과 lat={}, lng={}", lat, lng);
            return new GeoPoint(lat, lng);
        } catch (NullPointerException | NumberFormatException e) {
            log.error("좌표 데이터 파싱 실패: longitude={}, latitude={}", doc.getLongitude(), doc.getLatitude());
            throw new CoreException(ErrorType.ADDRESS_NOT_FOUND);
        }
    }

    /**
     * Client 엔티티의 주소를 기반으로 좌표를 변환하여 엔티티 필드를 직접 채웁니다.
     */
    public void fillCoordinates(com.monsoon.seedflowplus.domain.account.entity.Client client) {
        if (client == null || client.getAddress() == null || client.getAddress().isBlank()) {
            return;
        }
        GeoPoint point = getGeoPoint(client.getAddress());
        client.updateCoordinates(point.latitude(), point.longitude());
    }

    public record GeoPoint(double latitude, double longitude) {
    }
}
