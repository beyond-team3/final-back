package com.monsoon.seedflowplus.infra.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monsoon.seedflowplus.domain.note.entity.SalesBriefing;
import com.monsoon.seedflowplus.infra.ai.dto.GeminiApiResponse;
import com.monsoon.seedflowplus.infra.ai.dto.GeminiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiAiClient implements AiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${google.gemini.api.key}")
    private String apiKey;

    @Value("${google.gemini.api.url}")
    private String apiUrl;

    @Override
    public SalesBriefing analyzeSalesStrategy(Long clientId, String text) {
        try {
            String prompt = buildPromptWithCitation(text);

            // 1. API 요청 바디 구성 (snake_case 규격 준수)
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.1,
                            "response_mime_type", "application/json" // 필드명 수정
                    )
            );

            // 2. HTTP 헤더 설정 (보안 강화: x-goog-api-key 사용)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey); // API 키를 헤더로 이동

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 3. URI 생성 (쿼리 파라미터에서 키 제거)
            URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .build()
                    .toUri();

            // 4. Gemini API 호출
            ResponseEntity<GeminiApiResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.POST,
                    entity,
                    GeminiApiResponse.class
            );

            // 5. 응답 데이터 추출 및 DTO 변환
            String jsonText = response.getBody()
                    .getCandidates().get(0)
                    .getContent().getParts().get(0)
                    .getText();

            GeminiResponse aiResult = objectMapper.readValue(jsonText, GeminiResponse.class);

            // 6. 엔티티 생성 (camelCase 필드 및 JsonProperty 매핑 결과 반영)
            return SalesBriefing.builder()
                    .clientId(clientId)
                    .statusChange(aiResult.getStatusChange())
                    .longTermPattern(aiResult.getLongTermPattern())
                    .strategySuggestion(aiResult.getStrategySuggestion())
                    .evidenceNoteIds(aiResult.getEvidenceNoteIds())
                    .version(aiResult.getVersion())
                    .build();

            // GeminiAiClient.java의 catch 블록 수정 예시
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Gemini API 호출 타임아웃 발생: {}", e.getMessage());
            throw new RuntimeException("AI 분석 서버 응답 시간이 초과되었습니다.", e);
        } catch (Exception e) {
            log.error("Gemini 분석 중 알 수 없는 오류 발생: {}", e.getMessage());
            throw new RuntimeException("AI 전략 분석에 실패했습니다.", e);
        }
    }

    private String buildPromptWithCitation(String text) {
        return """
        당신은 종자 회사 전략 영업 컨설턴트입니다. 
        제공된 [회의록]을 분석하여 전략을 수립하세요. 
        각 전략의 근거가 된 회의록의 [ID]를 'evidence_note_ids'에 리스트 형태로 포함해야 합니다.

        반드시 아래 JSON 구조로만 응답하세요:
        {
          "status_change": ["최근 변화 내용"],
          "long_term_pattern": ["포착된 장기 패턴"],
          "strategy_suggestion": "실행 가능한 구체적 영업 전략",
          "evidence_note_ids": [101, 102],
          "version": "v1.1"
        }

        [회의록 데이터]
        """ + text;
    }
}