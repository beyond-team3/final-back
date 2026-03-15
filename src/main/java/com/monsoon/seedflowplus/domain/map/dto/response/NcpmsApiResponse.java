package com.monsoon.seedflowplus.domain.map.dto.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class NcpmsApiResponse {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Getter @Setter
    @JacksonXmlRootElement(localName = "service")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NcpmsListResponse {
        @JsonAlias({"item", "list", "structList"})
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "item")
        private List<NcpmsListDto> items = new ArrayList<>();

        public void setItems(JsonNode node) {
            this.items = parseInconsistentNode(node, new TypeReference<List<NcpmsListDto>>() {}, NcpmsListDto.class);
        }
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NcpmsListDto {
        private String insectKey;
        private String examinYear;
        private String kncrNm;
    }

    @Getter @Setter
    @JacksonXmlRootElement(localName = "service")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NcpmsSidoResponse {
        @JsonAlias({"item", "list", "structList"})
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "item")
        private List<NcpmsSidoDto> items = new ArrayList<>();

        public void setItems(JsonNode node) {
            this.items = parseInconsistentNode(node, new TypeReference<List<NcpmsSidoDto>>() {}, NcpmsSidoDto.class);
        }
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NcpmsSidoDto {
        private String insectKey;
        @JsonAlias({"sidoCd", "sido_code", "sido_cd", "sidoCode"})
        private String sidoCode;
        @JsonAlias({"sidoNm", "sido_name", "sido_nm", "sidoName"})
        private String sidoNm;
        @JsonAlias({"dbyhsNm", "pestNm", "pestName"})
        private String dbyhsNm;
        private Double inqireValue;
    }

    @Getter @Setter
    @JacksonXmlRootElement(localName = "service")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NcpmsSigunguResponse {
        @JsonAlias({"item", "list", "structList"})
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "item")
        private List<NcpmsSigunguDto> items = new ArrayList<>();

        public void setItems(JsonNode node) {
            this.items = parseInconsistentNode(node, new TypeReference<List<NcpmsSigunguDto>>() {}, NcpmsSigunguDto.class);
        }
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NcpmsSigunguDto {
        private String insectKey;
        @JsonAlias({"sidoCd", "sido_code", "sido_cd", "sidoCode"})
        private String sidoCode;
        @JsonAlias({"sigunguCd", "sigungu_code", "sigungu_cd", "sigunguCode"})
        private String sigunguCode;
        @JsonAlias({"sigunguNm", "sigungu_name", "sigungu_nm", "sigunguName"})
        private String sigunguNm;
        @JsonAlias({"dbyhsNm", "pestNm", "pestName"})
        private String dbyhsNm;
        private Double inqireValue;
    }

    private static <T> List<T> parseInconsistentNode(JsonNode node, TypeReference<List<T>> typeRef, Class<T> clazz) {
        if (node == null || node.isNull()) return new ArrayList<>();

        try {
            if (node.isArray()) {
                return mapper.convertValue(node, typeRef);
            } else if (node.isObject()) {
                List<T> result = new ArrayList<>();
                result.add(mapper.convertValue(node, clazz));
                return result;
            } else if (node.isTextual()) {
                String text = node.asText().trim();
                if (text.isEmpty()) return new ArrayList<>();

                // 1. 먼저 표준 JSON 파싱 시도
                try {
                    if (text.startsWith("[")) {
                        return mapper.readValue(text, typeRef);
                    } else if (text.startsWith("{")) {
                        List<T> result = new ArrayList<>();
                        result.add(mapper.readValue(text, clazz));
                        return result;
                    }
                } catch (Exception e) {
                    // 2. 표준 파싱 실패 시 복구 로직(key=value 형식) 시도
                    if (text.startsWith("[") || text.startsWith("{")) {
                        log.warn("Standard JSON parsing failed. Attempting recovery for: {}", text);
                        return parseMapToStringFormat(text, typeRef, clazz);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse node: {}", node, e);
        }
        return new ArrayList<>();
    }

    private static <T> List<T> parseMapToStringFormat(String text, TypeReference<List<T>> typeRef, Class<T> clazz) {
        try {
            // [{k1=v1, k2=v2}, {k1=v3}] -> [{"k1":"v1", "k2":"v2"}, {"k1":"v3"}]
            // 1. Handle keys: {key= or , key=
            String jsonified = text.replaceAll("([{, ])([a-zA-Z0-9_]+)=", "$1\"$2\":");
            
            // 2. Handle values: :value, or :value}
            // Use a more careful approach for values that might contain spaces
            Pattern valuePattern = Pattern.compile("(:)([^,}\\]]+)([,}\\]])");
            Matcher matcher = valuePattern.matcher(jsonified);
            StringBuilder sb = new StringBuilder();
            int lastEnd = 0;
            while (matcher.find()) {
                sb.append(jsonified, lastEnd, matcher.start());
                sb.append(":\"").append(matcher.group(2).trim()).append("\"").append(matcher.group(3));
                lastEnd = matcher.end();
            }
            sb.append(jsonified.substring(lastEnd));
            
            String finalJson = sb.toString();
            
            if (finalJson.startsWith("[")) {
                return mapper.readValue(finalJson, typeRef);
            } else {
                List<T> result = new ArrayList<>();
                result.add(mapper.readValue(finalJson, clazz));
                return result;
            }
        } catch (Exception e) {
            log.error("Manual recovery failed for: {}", text, e);
            return new ArrayList<>();
        }
    }
}
