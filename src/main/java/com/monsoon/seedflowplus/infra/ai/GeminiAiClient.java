package com.monsoon.seedflowplus.infra.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monsoon.seedflowplus.domain.note.entity.SalesBriefing;
import com.monsoon.seedflowplus.infra.ai.dto.GeminiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiAiClient implements AiClient {

    @Value("${google.gemini.api.key}")
    private String apiKey;

    @Value("${google.gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public SalesBriefing analyzeSalesStrategy(Long clientId, String text) {
        // 1. 프롬프트 구성 (회의록 기반 전략 수립 가이드라인 적용)
        String prompt = String.format("""
            당신은 B2B 전략 영업 컨설턴트입니다. 다음 영업 노트들을 분석하여 JSON 형식으로 응답하세요.

            분석 대상 노트:
            %s

            반드시 아래 JSON 구조로만 응답하세요:
            {
              "status_change": ["최근 변화 1", "최근 변화 2"],
              "long_term_pattern": ["장기 패턴 1", "장기 패턴 2"],
              "strategy_suggestion": "구체적인 영업 전략 제안",
              "version": "v1.0"
            }
            """, text);

        try {
            // 2. API 요청 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    )
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String fullUrl = apiUrl + "?key=" + apiKey;

            // 3. API 호출 및 결과 파싱
            ResponseEntity<String> response = restTemplate.postForEntity(fullUrl, entity, String.class);

            // 실제 응답 구조에 맞게 JSON 추출 (예시를 단순화함)
            GeminiResponse result = objectMapper.readValue(response.getBody(), GeminiResponse.class);

            // 4. 엔티티로 변환하여 반환
            return SalesBriefing.builder()
                    .clientId(clientId)
                    .statusChange(result.getStatus_change())
                    .longTermPattern(result.getLong_term_pattern())
                    .strategySuggestion(result.getStrategy_suggestion())
                    .version(result.getVersion())
                    .build();

        } catch (Exception e) {
            log.error("Gemini API 호출 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("AI 분석 실패", e);
        }
    }
}