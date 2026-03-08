package com.monsoon.seedflowplus.domain.sales.scheduler;

import com.monsoon.seedflowplus.domain.sales.contract.service.ContractService;
import com.monsoon.seedflowplus.domain.sales.quotation.service.QuotationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalesScheduler {

    private final ContractService contractService;
    private final QuotationService quotationService;

    /**
     * 매일 자정 1분에 실행 (00:01:00)
     * 계약서 및 견적서의 유효 기간에 맞춰 상태를 자동으로 업데이트합니다.
     */
    @Scheduled(cron = "0 1 0 * * *")
    public void autoSyncStatuses() {
        log.info("[SalesScheduler] 상태 자동 동기화 시작");
        try {
            contractService.syncContractStatuses();
        } catch (Exception e) {
            log.error("[SalesScheduler] 계약 상태 동기화 중 오류 발생", e);
        }

        try {
            quotationService.syncQuotationStatuses();
        } catch (Exception e) {
            log.error("[SalesScheduler] 견적 상태 동기화 중 오류 발생", e);
        }

        log.info("[SalesScheduler] 상태 자동 동기화 종료");
    }
}
