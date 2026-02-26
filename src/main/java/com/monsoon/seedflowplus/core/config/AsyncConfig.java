package com.monsoon.seedflowplus.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // 프로젝트 전체에 비동기 기능을 활성화합니다.
public class AsyncConfig {

    /**
     * AI 브리핑 분석 전용 스레드 풀 설정
     * BriefingService에서 @Async("briefingTaskExecutor")로 참조합니다.
     */
    @Bean(name = "briefingTaskExecutor")
    public Executor briefingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 핵심 스레드 수: 기본적으로 유지할 일꾼 수
        executor.setCorePoolSize(5);

        // 최대 스레드 수: 작업이 몰릴 때 최대로 늘릴 일꾼 수
        executor.setMaxPoolSize(10);

        // 큐 용량: 작업이 대기하는 줄의 길이
        executor.setQueueCapacity(100);

        // 로그에서 식별하기 위한 스레드 이름 접두사
        executor.setThreadNamePrefix("Briefing-Async-");

        // 애플리케이션 종료 시 진행 중인 작업을 완료할 때까지 대기하도록 설정
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }
}