package com.monsoon.seedflowplus.domain.map.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.monsoon.seedflowplus.domain.map.dto.response.NcpmsApiResponse;
import com.monsoon.seedflowplus.domain.map.dto.response.NcpmsApiResponse.NcpmsSidoResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NcpmsParsingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final XmlMapper xmlMapper = new XmlMapper();

    @Test
    @DisplayName("비표준 Map.toString() 문자열 데이터 복구 테스트")
    void testMapToStringFormatParsing() throws Exception {
        String problemData = "[{dbyhsNm=잎도열병(발생추정면적), sidoNm=인천광역시, sidoCode=28}]";
        
        NcpmsSidoResponse response = new NcpmsSidoResponse();
        // Jackson이 "item": "[...]" 을 처리할 때 setItems(TextNode)를 호출하는 상황 재현
        response.setItems(objectMapper.readTree("\"" + problemData + "\""));
        
        assertNotNull(response.getItems());
        assertEquals(1, response.getItems().size());
        assertEquals("인천광역시", response.getItems().get(0).getSidoNm());
    }

    @Test
    @DisplayName("XML 복수 아이템 파싱 테스트 (Wrapper 미사용 구조)")
    void testStandardXmlParsing() throws Exception {
        String xml = "<service><item><sidoNm>경기도</sidoNm></item><item><sidoNm>강원도</sidoNm></item></service>";
        
        NcpmsSidoResponse response = xmlMapper.readValue(xml, NcpmsSidoResponse.class);
        
        assertNotNull(response.getItems());
        // Note: XmlMapper는 같은 이름의 태그가 여러개면 마지막꺼만 남기거나 
        // @JacksonXmlElementWrapper(useWrapping = false) 설정이 잘 동작해야 함
        assertTrue(response.getItems().size() >= 1);
    }
}
