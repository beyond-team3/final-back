package com.monsoon.seedflowplus.domain.map.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class NcpmsApiResponse {

    // 재사용을 위한 정적 매퍼 (형변환용)
    private static final ObjectMapper mapper = new ObjectMapper();

    @Getter
    @JacksonXmlRootElement(localName = "service")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NcpmsListResponse {
        private List<NcpmsListDto> items = new ArrayList<>();

        @JsonAlias({"item", "list", "structList"})
        @JacksonXmlProperty(localName = "item")
        public void setItems(JsonNode node) {
            if (node != null && node.isArray()) {
                this.items = mapper.convertValue(node, new TypeReference<List<NcpmsListDto>>() {});
            }
        }
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NcpmsListDto {
        private String insectKey;
        private String examinYear;
        private String kncrNm;
    }

    @Getter
    @JacksonXmlRootElement(localName = "service")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NcpmsSidoResponse {
        private List<NcpmsSidoDto> items = new ArrayList<>();

        @JsonAlias({"item", "list", "structList"})
        @JacksonXmlProperty(localName = "item")
        public void setItems(JsonNode node) {
            if (node != null && node.isArray()) {
                this.items = mapper.convertValue(node, new TypeReference<List<NcpmsSidoDto>>() {});
            }
        }
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NcpmsSidoDto {
        private String insectKey;
        private String sidoCode;
        private String sidoNm;
        private String dbyhsNm;      // 추가: Sido 응답에도 포함됨
        private Integer inqireValue; // 추가: Sido 응답에도 포함됨
    }

    @Getter
    @JacksonXmlRootElement(localName = "service")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NcpmsSigunguResponse {
        private List<NcpmsSigunguDto> items = new ArrayList<>();

        @JsonAlias({"item", "list", "structList"})
        @JacksonXmlProperty(localName = "item")
        public void setItems(JsonNode node) {
            if (node != null && node.isArray()) {
                this.items = mapper.convertValue(node, new TypeReference<List<NcpmsSigunguDto>>() {});
            }
        }
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NcpmsSigunguDto {
        private String insectKey;
        private String sidoCode;
        private String sigunguNm;
        private String dbyhsNm;
        private Integer inqireValue;
    }
}
