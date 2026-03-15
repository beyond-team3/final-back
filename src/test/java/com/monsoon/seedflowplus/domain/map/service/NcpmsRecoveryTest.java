//package com.monsoon.seedflowplus.domain.map.service;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.monsoon.seedflowplus.domain.map.dto.response.NcpmsApiResponse.NcpmsSigunguResponse;
//import com.monsoon.seedflowplus.domain.map.dto.response.NcpmsApiResponse.NcpmsSigunguDto;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class NcpmsRecoveryTest {
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    @Test
//    @DisplayName("비표준 데이터 복구 시 null 및 숫자 값 처리 테스트")
//    void testRecoveryWithNullAndNumericValues() throws Exception {
//        // Log-like sample: {dbyhsNm=부란병, inqireValue=null, sigunguNm=정읍시, sigunguCode=4518, numericVal=1.76}
//        String problemData = "[{dbyhsNm=부란병, inqireValue=null, sigunguNm=정읍시, sigunguCode=4518, inqireValue2=1.76}]";
//
//        NcpmsSigunguResponse response = new NcpmsSigunguResponse();
//        // Simulating Jackson's behavior when "item" is a TextNode
//        JsonNode node = objectMapper.readTree("\"" + problemData + "\"");
//        response.setItems(node);
//
//        List<NcpmsSigunguDto> items = response.getItems();
//        assertNotNull(items);
//        assertEquals(1, items.size());
//
//        NcpmsSigunguDto dto = items.get(0);
//        assertEquals("부란병", dto.getDbyhsNm());
//        assertEquals("정읍시", dto.getSigunguNm());
//        assertEquals("4518", dto.getSigunguCode());
//        assertNull(dto.getInqireValue());
//        // Since NcpmsSigunguDto doesn't have inqireValue2, it won't be mapped, but we checked inqireValue=null
//    }
//}
