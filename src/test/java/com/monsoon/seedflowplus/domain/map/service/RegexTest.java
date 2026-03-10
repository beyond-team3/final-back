package com.monsoon.seedflowplus.domain.map.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.monsoon.seedflowplus.domain.map.dto.response.NcpmsApiResponse.NcpmsSidoDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class RegexTest {

    @Test
    void testImprovedRegex() throws Exception {
        String text = "[{dbyhsNm=잎도열병(발생추정면적), inqireCnClCode=SF0001, sidoNm=인천광역시, inqireValue=0, sidoCode=28}]";
        
        String jsonified = text.replaceAll("([{, ])([a-zA-Z0-9_]+)=", "$1\"$2\":");
        
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
        System.out.println("FINAL JSON: " + finalJson);
        
        ObjectMapper mapper = new ObjectMapper();
        List<NcpmsSidoDto> list = mapper.readValue(finalJson, new TypeReference<List<NcpmsSidoDto>>() {});
        assertEquals(1, list.size());
        assertEquals("인천광역시", list.get(0).getSidoNm());
        assertEquals("잎도열병(발생추정면적)", list.get(0).getDbyhsNm());
    }
}
