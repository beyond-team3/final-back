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
    public SalesBriefing analyzeSalesStrategy(Long clientId, List<TextSegment> contexts, String scopeDescription) {
        try {
            String notesText = formatContexts(contexts, "SALES_NOTE");
            String productsText = formatContexts(contexts, "PRODUCT_CATALOG");

            String augmentedPrompt = buildAugmentedPrompt(notesText, productsText, scopeDescription);

            Map<String, Object> requestBody = createRequestBody(augmentedPrompt, 0.1);
            String jsonText = callGemini(requestBody);

            GeminiResponse aiResult = objectMapper.readValue(jsonText, GeminiResponse.class);

            return SalesBriefing.builder()
                    .clientId(clientId)
                    .statusChange(aiResult.getStatusChange())
                    .longTermPattern(aiResult.getLongTermPattern())
                    .strategySuggestion(aiResult.getStrategySuggestion())
                    .evidenceNoteIds(aiResult.getEvidenceNoteIds())
                    .version("RAGseed-Standard-v1.3")
                    .build();

        } catch (Exception e) {
            log.error("RAGseed 표준 분석 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("AI 전략 분석에 실패했습니다.", e);
        }
    }

    /**
     * RAGseed 타겟팅 전략 생성
     */
    @Override
    public String generateTargetedResponse(String userPrompt, List<TextSegment> contexts, String scopeDescription) {
        try {
            String contextText = contexts.stream()
                    .map(s -> String.format("[%s] %s", s.metadata().get("type"), s.text()))
                    .collect(Collectors.joining("\n"));

            String fullPrompt = String.format("""
                당신은 전략 인출 엔진 'RAGseed'입니다. 
                현재 분석 범위는 [%s]입니다.
                제공된 영업 데이터 자산(Seed)에서 사용자의 요청에 대한 가장 정확한 전략을 인출(Retrieval)하세요.
                
                [인출 데이터]
                %s
                
                [사용자 요청]
                %s
                
                [지침]
                - 답변 서두에 반드시 "본 분석은 %s를 바탕으로 도출되었습니다."라는 문구를 포함하세요.
                - 신뢰감 있고 전문적인 B2B 영업 어조를 유지하세요.
                - 인출된 데이터에 기반하여 구체적인 수치나 사례가 있다면 반드시 언급하세요.
                - 데이터에 없는 내용은 추측하지 마세요.
                """, scopeDescription, contextText, userPrompt, scopeDescription);

            Map<String, Object> requestBody = createRequestBody(fullPrompt, 0.2);
            return callGemini(requestBody);

        } catch (Exception e) {
            log.error("RAGseed 타겟 전략 생성 중 오류: {}", e.getMessage());
            return "전략 인출에 실패했습니다. 데이터를 다시 확인해주세요.";
        }
    }

    private String formatContexts(List<TextSegment> contexts, String type) {
        return contexts.stream()
                .filter(s -> type.equals(s.metadata().get("type")))
                .map(s -> {
                    if ("SALES_NOTE".equals(type)) {
                        return String.format("[ID: %s] (%s) %s", s.metadata().get("id"), s.metadata().get("activityDate"), s.text());
                    } else {
                        return String.format("[ID: %s] (%s) %s", s.metadata().get("productId"), s.metadata().get("category"), s.text());
                    }
                })
                .collect(Collectors.joining("\n"));
    }

    private Map<String, Object> createRequestBody(String prompt, double temp) {
        return Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("temperature", temp, "response_mime_type", "application/json")
        );
    }

    private String callGemini(Map<String, Object> requestBody) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl).build().toUri();

        ResponseEntity<GeminiApiResponse> response = restTemplate.exchange(uri, HttpMethod.POST, entity, GeminiApiResponse.class);
        String text = extractTextFromResponse(response.getBody()).orElseThrow();
        
        if (text.startsWith("```")) {
            text = text.replaceAll("(?s)```(?:json)?\\n?(.*?)\\n?```", "$1").trim();
        }
        return text;
    }

    private String buildAugmentedPrompt(String notes, String products, String scope) {
        return String.format("""
        당신은 전략 영업 인출 엔진 'RAGseed'입니다.
        현재 분석 범위는 [%s]입니다.
        과거 데이터(Seed)에서 최적의 전략을 인출하여 표준 영업 브리핑을 작성하세요.

        [지침]
        1. 답변의 첫 문장은 반드시 "본 분석은 %s를 바탕으로 도출되었습니다."로 시작하세요.
        2. 과거 영업 기록을 분석하여 핵심 변화와 패턴을 도출하세요.
        3. 종자 카탈로그를 참조하여 고객에게 가장 적합한 품종을 전략적으로 제안하세요.

        반드시 아래 JSON 구조로만 응답하세요:
        {
          "status_change": ["최근 변화 리스트"],
          "long_term_pattern": ["장기 패턴 리스트"],
          "strategy_suggestion": "전문적인 영업 전략 제안",
          "evidence_note_ids": [101, 102],
          "version": "RAGseed-Standard-v1.3"
        }

        [데이터: Seed - 영업 기록]
        %s

        [데이터: Seed - 종자 카탈로그]
        %s
        """, scope, scope, notes, products);
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
