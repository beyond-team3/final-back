package com.monsoon.seedflowplus.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class GeminiResponse {

    @JsonProperty("status_change")
    private List<String> statusChange;

    @JsonProperty("long_term_pattern")
    private List<String> longTermPattern;

    @JsonProperty("strategy_suggestion")
    private String strategySuggestion;

    @JsonProperty("evidence_note_ids")
    private List<Long> evidenceNoteIds;

    private String version;
}