package com.monsoon.seedflowplus.domain.billing.invoice.scheduler;

import com.monsoon.seedflowplus.domain.billing.invoice.service.InvoiceService;
import com.monsoon.seedflowplus.domain.sales.contract.entity.BillingCycle;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractHeader;
import com.monsoon.seedflowplus.domain.sales.contract.entity.ContractStatus;
import com.monsoon.seedflowplus.domain.sales.contract.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceScheduler {

    private final InvoiceService invoiceService;
    private final ContractRepository contractHeaderRepository;

    /**
     * 매일 자정 실행
     * 오늘이 각 계약의 billing_cycle 기준 청구일(말일)이면 DRAFT 자동 생성
     *
     * MONTHLY    → 매달 말일
     * QUARTERLY  → 3, 6, 9, 12월 말일
     * HALF_YEARLY→ 6, 12월 말일
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void autoCreateDraftInvoices() {
        LocalDate today = LocalDate.now();

        // 말일이 아니면 스킵
        if (!isLastDayOfMonth(today)) {
            return;
        }

        // 계약 완료(COMPLETED) 상태인 계약 전체 조회 (기간 체크는 아래서 수행)
        List<ContractHeader> activeContracts =
                contractHeaderRepository.findAllByStatus(ContractStatus.COMPLETED);

        for (ContractHeader contract : activeContracts) {
            // 계약 기간 내인지 확인
            if (today.isBefore(contract.getStartDate()) || today.isAfter(contract.getEndDate())) {
                continue;
            }

            if (isBillingDay(today, contract.getBillingCycle())) {
                try {
                    invoiceService.createDraftInvoice(contract);
                    log.info("[InvoiceScheduler] DRAFT 생성 완료 - contractId={}", contract.getId());
                } catch (Exception e) {
                    // 하나 실패해도 다른 계약에 영향 없도록 예외 격리
                    log.error("[InvoiceScheduler] DRAFT 생성 실패 - contractId={}, error={}",
                            contract.getId(), e.getMessage());
                }
            }
        }
    }

    // 오늘이 해당 billing_cycle의 청구일인지 판단
    private boolean isBillingDay(LocalDate today, BillingCycle billingCycle) {
        int month = today.getMonthValue();
        return switch (billingCycle) {
            case MONTHLY -> true; // 말일이면 무조건
            case QUARTERLY -> month == 3 || month == 6 || month == 9 || month == 12;
            case HALF_YEARLY -> month == 6 || month == 12;
        };
    }

    // 말일 여부 확인
    private boolean isLastDayOfMonth(LocalDate date) {
        return date.getDayOfMonth() == date.lengthOfMonth();
    }
}