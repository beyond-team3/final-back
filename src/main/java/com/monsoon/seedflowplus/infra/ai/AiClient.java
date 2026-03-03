package com.monsoon.seedflowplus.infra.ai;

import com.monsoon.seedflowplus.domain.note.entity.SalesBriefing;
import java.util.List;

public interface AiClient {
    /**
     * 누적 미팅 메모를 분석하여 전략 리포트 객체를 생성합니다.
     */
    SalesBriefing analyzeSalesStrategy(Long clientId, String text);

    /**
     * 단일 영업 활동 내용을 3문장 이내로 요약합니다.
     */
    List<String> summarizeNote(String content);
}