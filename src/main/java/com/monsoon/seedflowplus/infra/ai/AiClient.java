package com.monsoon.seedflowplus.infra.ai;

import com.monsoon.seedflowplus.domain.note.entity.SalesBriefing;
import dev.langchain4j.data.segment.TextSegment;
import java.util.List;

/**
 * RAGseed 통합 AI 엔진 인터페이스입니다.
 */
public interface AiClient {
    /**
     * 표준 영업 브리핑 리포트를 생성합니다.
     */
    SalesBriefing analyzeSalesStrategy(Long clientId, List<TextSegment> contexts, String scopeDescription);

    /**
     * 특정 목적(Targeted)에 따른 맞춤형 전략을 인출합니다.
     */
    String generateTargetedResponse(String prompt, List<TextSegment> contexts, String scopeDescription);

    /**
     * 단일 영업 활동 내용을 요약합니다.
     */
    List<String> summarizeNote(String content);
}