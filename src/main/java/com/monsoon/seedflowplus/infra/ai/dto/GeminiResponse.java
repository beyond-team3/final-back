package com.monsoon.seedflowplus.infra.ai.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class GeminiResponse {
    private List<String> status_change;
    private List<String> long_term_pattern;
    private String strategy_suggestion;
    private String version;
}