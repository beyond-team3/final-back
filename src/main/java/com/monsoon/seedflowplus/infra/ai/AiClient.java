package com.monsoon.seedflowplus.infra.ai;

import com.monsoon.seedflowplus.domain.note.entity.SalesBriefing;

public interface AiClient {
    /**
     * 누적 미팅 메모를 분석하여 전략 리포트 객체를 생성합니다.
     */
    SalesBriefing analyzeSalesStrategy(Long clientId, String text);
}