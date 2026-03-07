package com.monsoon.seedflowplus.domain.sales.contract.scheduler;

import com.monsoon.seedflowplus.domain.sales.contract.service.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContractScheduler {

    private final ContractService contractService;

    /**
     * 매일 자정 1분에 실행 (00:01:00)
     * 계약서의 시작일과 종료일에 맞춰 상태를 자동으로 업데이트합니다.
     */
    @Scheduled(cron = "0 1 0 * * *")
    public void autoSyncContractStatuses() {
        log.info("[ContractScheduler] 계약 상태 자동 동기화 시작");
        try {
            contractService.syncContractStatuses();
            log.info("[ContractScheduler] 계약 상태 자동 동기화 완료");
        } catch (Exception e) {
            log.error("[ContractScheduler] 계약 상태 자동 동기화 중 오류 발생: {}", e.getMessage());
        }
    }
}
