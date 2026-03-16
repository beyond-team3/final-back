package com.monsoon.seedflowplus.domain.scoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScoringScheduler {

    private final ScoringService scoringService;

    /**
     * 서버 기동 시점에 점수 초기 계산을 수행합니다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("애플리케이션 시작: 초기 고객 점수 계산을 시작합니다.");
        scoringService.updateAllAccountScores();
    }

    /**
     * 매 시 정각마다 고객 관리 우선순위 점수를 갱신합니다.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void runScoringBatch() {
        log.info("고객 관리 우선순위 점수 배치 갱신 시작: {}", LocalDateTime.now());
        try {
            scoringService.updateAllAccountScores();
            log.info("고객 관리 우선순위 점수 배치 갱신 완료");
        } catch (Exception e) {
            log.error("고객 관리 우선순위 점수 갱신 중 오류 발생", e);
        }
    }
}
