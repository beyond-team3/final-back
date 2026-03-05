package com.monsoon.seedflowplus.infra.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class KakaoGeoResponse {
    private List<Document> documents;

    @Getter
    @Setter
    public static class Document {
        @JsonProperty("x")
        private String longitude; // 경도

        @JsonProperty("y")
        private String latitude;  // 위도
    }
}