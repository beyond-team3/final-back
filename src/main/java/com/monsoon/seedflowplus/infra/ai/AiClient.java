package com.monsoon.seedflowplus.infra.ai;

import com.monsoon.seedflowplus.domain.note.entity.SalesBriefing;
import dev.langchain4j.data.segment.TextSegment;
import java.util.List;

public interface AiClient {
    /**
     * 검색된 과거 노트(Context)를 바탕으로 전략 리포트 객체를 생성합니다.
     */
    SalesBriefing analyzeSalesStrategy(Long clientId, List<TextSegment> contexts);

    /**
     * 단일 영업 활동 내용을 3문장 이내로 요약합니다.
     */
    List<String> summarizeNote(String content);
}