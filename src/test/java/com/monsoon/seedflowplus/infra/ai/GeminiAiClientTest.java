package com.monsoon.seedflowplus.infra.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monsoon.seedflowplus.domain.note.entity.SalesBriefing;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        GeminiAiClient.class,
        RestTemplateConfig.class,
        ObjectMapper.class
})
@ActiveProfiles("test") // test 프로필의 API Key와 URL을 사용
class GeminiAiClientTest {

    @Autowired
    private GeminiAiClient geminiAiClient;

    @Test
    @DisplayName("실제 Gemini API를 호출하여 분석 결과와 근거 ID가 포함된 브리핑을 생성한다")
    void analyzeSalesStrategy_RealApiCall() {
        // Given: ID가 포함된 테스트용 회의록 데이터
        Long clientId = 100L;
        String mockNotes = """
                [ID: 101] (날짜: 2026-02-24) 내용: 고객이 A 품종의 내병성에 큰 관심을 보임.
                [ID: 102] (날짜: 2026-02-25) 내용: 경쟁사의 B 품종 단가 정보를 물어보며 가격 협상을 시도함.
                """;

        // When: 실제 API 호출
        SalesBriefing result = geminiAiClient.analyzeSalesStrategy(clientId, mockNotes);

        // Then: 반환된 엔티티 검증
        assertThat(result).isNotNull();
        assertThat(result.getClientId()).isEqualTo(clientId);

        // 1. 전략 제안 및 현황 데이터 검증
        assertThat(result.getStrategySuggestion()).isNotEmpty();
        assertThat(result.getStatusChange()).isNotEmpty();

        // 2. 핵심: 근거 추출(Citation) 검증
        // AI가 [ID: 101] 혹은 [ID: 102]를 인식하여 리스트에 담았는지 확인
        assertThat(result.getEvidenceNoteIds())
                .as("AI가 분석 근거로 사용한 노트 ID를 추출해야 함")
                .containsAnyOf(101L, 102L);

        System.out.println("AI 전략 제안: " + result.getStrategySuggestion());
        System.out.println("분석 근거 ID 리스트: " + result.getEvidenceNoteIds());
    }
}