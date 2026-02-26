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
        System.out.println("주입된 API Key 확인: " + apiKey);
        try {
            // 1. 근거 추출을 위한 가이드가 포함된 프롬프트 생성
            String prompt = buildPromptWithCitation(text);

            // 2. API 요청 바디 구성 (JSON 모드 활성화)
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.1, // 일관된 분석을 위해 온도를 낮춤
                            "responseMimeType", "application/json"
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 3. Gemini API 호출
            URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                    .queryParam("key", apiKey)
                    .build(true) // 인코딩된 상태로 유지
                    .toUri();

            ResponseEntity<GeminiApiResponse> response = restTemplate.exchange(
                    uri,
                    HttpMethod.POST,
                    entity,
                    GeminiApiResponse.class
            );

            // 4. 응답 데이터 추출 (JSON 텍스트 추출)
            String jsonText = response.getBody()
                    .getCandidates().get(0)
                    .getContent().getParts().get(0)
                    .getText();

            // 5. 비즈니스 DTO로 변환 (근거 ID 포함)
            GeminiResponse aiResult = objectMapper.readValue(jsonText, GeminiResponse.class);

            // 6. 엔티티 생성 시 근거 ID(evidenceNoteIds) 반영
            return SalesBriefing.builder()
                    .clientId(clientId)
                    .statusChange(aiResult.getStatus_change())
                    .longTermPattern(aiResult.getLong_term_pattern())
                    .strategySuggestion(aiResult.getStrategy_suggestion())
                    .evidenceNoteIds(aiResult.getEvidence_note_ids()) // 근거 필드 추가
                    .version(aiResult.getVersion())
                    .build();

        } catch (Exception e) {
            log.error("Gemini 분석 중 오류 발생: {}", e.getMessage());
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