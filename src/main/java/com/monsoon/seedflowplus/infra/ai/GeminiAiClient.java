package com.monsoon.seedflowplus.infra.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monsoon.seedflowplus.domain.note.entity.SalesBriefing;
import com.monsoon.seedflowplus.infra.ai.dto.GeminiApiResponse;
import com.monsoon.seedflowplus.infra.ai.dto.GeminiResponse;
import dev.langchain4j.data.segment.TextSegment;
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
import java.util.Optional;
import java.util.stream.Collectors;

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
    public SalesBriefing analyzeSalesStrategy(Long clientId, List<TextSegment> contexts) {
        try {
            // 1. 컨텍스트 타입별 분류 및 텍스트 조립
            String notesText = contexts.stream()
                    .filter(s -> "SALES_NOTE".equals(s.metadata().get("type")))
                    .map(s -> String.format("[영업노트 ID: %s] (%s) %s",
                            s.metadata().get("id"),
                            s.metadata().get("activityDate"),
                            s.text()))
                    .collect(Collectors.joining("\n"));

            String productsText = contexts.stream()
                    .filter(s -> "PRODUCT_CATALOG".equals(s.metadata().get("type")))
                    .map(s -> String.format("[품종 ID: %s] (카테고리: %s) %s",
                            s.metadata().get("productId"),
                            s.metadata().get("category"),
                            s.text()))
                    .collect(Collectors.joining("\n"));

            String augmentedPrompt = buildAugmentedPrompt(notesText, productsText);

            // 2. API 요청 바디 구성
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", augmentedPrompt)))
                    ),
                    "generationConfig", Map.of(
                            "temperature", 0.1,
                            "response_mime_type", "application/json"
                    )
            );

            // 3. HTTP 호출 및 결과 처리 (기존 로직 유지)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl).build().toUri();

            ResponseEntity<GeminiApiResponse> response = restTemplate.exchange(
                    uri, HttpMethod.POST, entity, GeminiApiResponse.class
            );

            String jsonText = extractTextFromResponse(response.getBody())
                    .orElseThrow(() -> new RuntimeException("Gemini API 응답 추출 실패"));

            GeminiResponse aiResult = objectMapper.readValue(jsonText, GeminiResponse.class);

            return SalesBriefing.builder()
                    .clientId(clientId)
                    .statusChange(aiResult.getStatusChange())
                    .longTermPattern(aiResult.getLongTermPattern())
                    .strategySuggestion(aiResult.getStrategySuggestion())
                    .evidenceNoteIds(aiResult.getEvidenceNoteIds())
                    .version(aiResult.getVersion())
                    .build();

        } catch (Exception e) {
            log.error("RAG 기반 Gemini 분석 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("AI 전략 분석에 실패했습니다.", e);
        }
    }

    private String buildAugmentedPrompt(String notes, String products) {
        return String.format("""
        당신은 종자 회사 전문 전략 영업 컨설턴트입니다.
        제공된 [과거 영업 기록]과 [최신 종자 카탈로그]를 분석하여 최적의 영업 전략을 수립하세요.

        [지침]
        1. [과거 영업 기록]을 바탕으로 고객의 최근 변화와 장기적인 패턴을 파악하세요.
        2. [최신 종자 카탈로그] 정보 중에서 고객의 현재 상황이나 문제 해결에 가장 적합한 품종을 최소 1개 이상 추천하세요.
        3. 'strategy_suggestion' 섹션에 추천 품종의 이름과 추천하는 구체적인 이유(예: 내병성, 수확량 등)를 포함하세요.
        4. 분석의 근거가 된 영업 기록의 ID들을 'evidence_note_ids'에 리스트 형태로 포함하세요.

        반드시 아래 JSON 구조로만 응답하세요:
        {
          "status_change": ["최근 변화 내용 리스트"],
          "long_term_pattern": ["포착된 장기 패턴 리스트"],
          "strategy_suggestion": "추천 품종 및 구체적 영업 전략 설명",
          "evidence_note_ids": [101, 102],
          "version": "v1.2-RAG"
        }

        [데이터: 과거 영업 기록]
        %s

        [데이터: 최신 종자 카탈로그]
        %s
        """, notes, products);
    }

    @Override
    public List<String> summarizeNote(String content) {
        try {
            String prompt = "다음 영업 미팅 내용을 핵심 위주로 3문장 이내의 리스트로 요약해줘. " +
                    "응답은 반드시 [\"요약1\", \"요약2\", \"요약3\"] 형식의 JSON 문자열 배열이어야 해. 다른 설명 없이 배열만 반환해.\n\n" +
                    "미팅 내용: " + content;

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                    "generationConfig", Map.of(
                            "temperature", 0.2,
                            "response_mime_type", "application/json"
                    )
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl).build().toUri();

            ResponseEntity<GeminiApiResponse> response = restTemplate.exchange(
                    uri, HttpMethod.POST, entity, GeminiApiResponse.class
            );

            // 응답 데이터 추출 (방어적 코드 적용)
            String jsonText = extractTextFromResponse(response.getBody())
                    .orElseThrow(() -> new RuntimeException("Gemini API로부터 유효한 요약 응답을 받지 못했습니다."));
            
            // [추가] 마크다운 기호(```json 등) 제거 로직
            if (jsonText.startsWith("```")) {
                jsonText = jsonText.replaceAll("(?s)```(?:json)?\\n?(.*?)\\n?```", "$1").trim();
            }

            if (log.isDebugEnabled()) {
                log.debug("Gemini 요약 결과 정제 전: {}", jsonText);
            }
            List<String> summary = objectMapper.readValue(jsonText, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            
            // 리스트 크기를 3개로 맞춤 (부족하면 빈 문자열 추가)
            while (summary.size() < 3) {
                summary.add("");
            }
            return summary.subList(0, 3); // 3개 초과시 절삭

        } catch (Exception e) {
            log.error("Gemini 요약 생성 중 상세 오류 발생: ", e); // 전체 스택트레이스 출력
            return List.of("요약 생성에 실패했습니다.", "AI 응답을 받지 못했습니다.", "내용을 직접 확인해주세요.");
        }
    }

    /**
     * Gemini API 응답 객체로부터 텍스트 내용을 안전하게 추출합니다.
     */
    private Optional<String> extractTextFromResponse(GeminiApiResponse response) {
        return Optional.ofNullable(response)
                .map(GeminiApiResponse::getCandidates)
                .filter(candidates -> !candidates.isEmpty())
                .map(candidates -> candidates.get(0))
                .map(GeminiApiResponse.Candidate::getContent)
                .map(GeminiApiResponse.Content::getParts)
                .filter(parts -> !parts.isEmpty())
                .map(parts -> parts.get(0))
                .map(GeminiApiResponse.Part::getText);
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
