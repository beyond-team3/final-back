package com.monsoon.seedflowplus.infra.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // 1. 연결 타임아웃: 서버와 연결을 맺는 시간 (5초)
        factory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());

        // 2. 읽기 타임아웃: 연결 후 응답 데이터를 받는 시간 (30초)
        // Gemini 분석은 복잡할 수 있으므로 읽기 시간은 넉넉하게 설정합니다.
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());

        return new RestTemplate(factory);
    }
}