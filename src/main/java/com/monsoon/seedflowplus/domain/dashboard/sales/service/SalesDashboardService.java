package com.monsoon.seedflowplus.domain.dashboard.sales.service;

import com.monsoon.seedflowplus.core.common.support.error.CoreException;
import com.monsoon.seedflowplus.core.common.support.error.ErrorType;
import com.monsoon.seedflowplus.domain.dashboard.sales.dto.*;
import com.monsoon.seedflowplus.domain.dashboard.sales.repository.SalesDashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesDashboardService {

    private final SalesDashboardRepository repo;

    private static final int BAR_MAX_HEIGHT = 80;   // Vue .monthly-bar-chart height
    private static final int BAR_MONTHS = 6;         // 바 차트 표시 개월 수
    private static final List<String> TIMELINE_FILTERS = List.of("전체", "견적", "계약", "주문");

    // doc_type → 한글
    private static final Map<String, String> DOC_LABEL = Map.of(
            "QUO", "견적 발송",
            "CNT", "계약 수정",
            "ORD", "주문 발생",
            "INV", "청구 발행",
            "RFQ", "견적 요청",
            "STMT", "명세서 발행",
            "PAY", "결제 완료"
    );

    // to_stage → CSS state
    private static final Set<String> COMPLETED_STAGES = Set.of(
            "COMPLETED", "PAID", "CONFIRMED", "APPROVED"
    );

    public SalesDashboardResponse getDashboard(Long employeeId) {
        String employeeName;
        try {
            employeeName = repo.findEmployeeName(employeeId);
        } catch (java.util.NoSuchElementException e) {
            throw new CoreException(ErrorType.EMPLOYEE_NOT_FOUND);
        }
        LocalDate today = LocalDate.now();
        LocalDate thisMonthStart = today.withDayOfMonth(1);
        LocalDate thisMonthEnd   = today.withDayOfMonth(today.lengthOfMonth());

        LocalDate prevMonthStart = thisMonthStart.minusMonths(1);
        LocalDate prevMonthEnd   = prevMonthStart.withDayOfMonth(prevMonthStart.lengthOfMonth());

        // ── 1. 이번달 / 전월 매출 ──────────────────
        BigDecimal thisMonthSales = repo.sumPaidInvoice(employeeId, thisMonthStart, thisMonthEnd);
        BigDecimal prevMonthSales = repo.sumPaidInvoice(employeeId, prevMonthStart, prevMonthEnd);

        // ── 2. 증감 계산 ──────────────────────────
        BigDecimal diff = thisMonthSales.subtract(prevMonthSales);

        double growthRate = 0.0;
        if (prevMonthSales.compareTo(BigDecimal.ZERO) != 0) {
            growthRate = diff.divide(prevMonthSales, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        // ── 3. 완료 계약 건수 ──────────────────────
        int completedCount = repo.countCompletedContracts(employeeId, thisMonthStart, thisMonthEnd);

        // ── 4. 바 차트 (최근 6개월) ────────────────
        LocalDate barFrom = thisMonthStart.minusMonths(BAR_MONTHS - 1);
        List<Map<String, Object>> rawBars = repo.monthlyPaidSales(employeeId, barFrom, thisMonthEnd);
        List<MonthlyBarResponse> monthlyBars = buildBarChart(rawBars, barFrom, today);

        // ── 5. 청구 대상 계약 ──────────────────────
        List<Map<String, Object>> rawBillings = repo.billingTargetsThisMonth(
                employeeId, thisMonthStart, thisMonthEnd);
        List<BillingTargetResponse> billings = rawBillings.stream()
                .map(this::toBillingResponse)
                .toList();

        // ── 6. 최근 영업 히스토리 ──────────────────
        List<Map<String, Object>> rawActivities = repo.recentActivities(employeeId);
        List<ActivityResponse> timeline = rawActivities.stream()
                .map(this::toActivityResponse)
                .toList();

        // ── 7. 조립 ───────────────────────────────
        return SalesDashboardResponse.builder()
                .header(DashboardHeaderResponse.builder()
                        .title(employeeName + "님의 영업 현황")
                        .subtitle(today.getYear() + "년 " + today.getMonthValue() + "월 기준")
                        .build())
                .monthlySales(MonthlySalesResponse.builder()
                        .periodLabel(today.getYear() + "년 " + today.getMonthValue() + "월")
                        .amount(formatAmount(thisMonthSales))
                        .change(formatGrowthRate(growthRate))
                        .diff(formatDiff(diff))
                        .completedCount(completedCount + "건")
                        .build())
                .monthlyBars(monthlyBars)
                .billings(billings)
                .timeline(timeline)
                .timelineFilters(TIMELINE_FILTERS)
                .build();
    }

    // ──────────────────────────────────────────────
    // 바 차트 정규화
    // ──────────────────────────────────────────────

    private List<MonthlyBarResponse> buildBarChart(List<Map<String, Object>> rawBars,
                                                   LocalDate barFrom,
                                                   LocalDate today) {
        // 조회 결과를 year_month → total 맵으로 변환
        Map<String, BigDecimal> salesByMonth = new LinkedHashMap<>();
        for (Map<String, Object> row : rawBars) {
            salesByMonth.put((String) row.get("year_month"),
                    (BigDecimal) row.get("total"));
        }

        // 6개월치 슬롯 생성 (데이터 없는 달 → 0)
        List<String> slots = new ArrayList<>();
        for (int i = 0; i < BAR_MONTHS; i++) {
            LocalDate m = barFrom.plusMonths(i);
            slots.add(String.format("%d-%02d", m.getYear(), m.getMonthValue()));
        }

        List<BigDecimal> amounts = slots.stream()
                .map(s -> salesByMonth.getOrDefault(s, BigDecimal.ZERO))
                .toList();

        // 최대값 기준 height 정규화
        BigDecimal max = amounts.stream()
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ONE);
        if (max.compareTo(BigDecimal.ZERO) == 0) max = BigDecimal.ONE;

        String thisMonthKey = String.format("%d-%02d", today.getYear(), today.getMonthValue());

        List<MonthlyBarResponse> result = new ArrayList<>();
        for (int i = 0; i < BAR_MONTHS; i++) {
            String slot = slots.get(i);
            BigDecimal amount = amounts.get(i);
            int height = amount.multiply(BigDecimal.valueOf(BAR_MAX_HEIGHT))
                    .divide(max, 0, RoundingMode.HALF_UP)
                    .intValue();

            // 월 레이블: "3월" 형식
            String[] parts = slot.split("-");
            String monthLabel = Integer.parseInt(parts[1]) + "월";

            result.add(MonthlyBarResponse.builder()
                    .month(monthLabel)
                    .height(height)
                    .current(slot.equals(thisMonthKey))
                    .build());
        }
        return result;
    }

    // ──────────────────────────────────────────────
    // 청구 대상 → DTO 변환
    // ──────────────────────────────────────────────

    private BillingTargetResponse toBillingResponse(Map<String, Object> row) {
        String statusRaw = (String) row.get("status");
        // invoice 가 아직 없는 경우(LEFT JOIN → NULL) 는 "청구 예정" / pending
        boolean hasInvoice = statusRaw != null;

        String docNo   = hasInvoice ? (String) row.get("doc_no") : "-";
        String amount  = hasInvoice
                ? formatAmount((BigDecimal) row.get("total_amount"))
                : "-";
        String status;
        String type;

        if (!hasInvoice || "DRAFT".equals(statusRaw)) {
            status = "청구 예정";
            type   = "pending";
        } else { // PUBLISHED
            status = "발행 완료";
            type   = "ready";
        }

        return BillingTargetResponse.builder()
                .docNo(docNo)
                .client((String) row.get("client_name"))
                .amount(amount)
                .status(status)
                .type(type)
                .build();
    }

    // ──────────────────────────────────────────────
    // 영업 히스토리 → DTO 변환
    // ──────────────────────────────────────────────

    private ActivityResponse toActivityResponse(Map<String, Object> row) {
        String docType    = (String) row.get("doc_type");
        String clientName = (String) row.get("client_name");
        String targetCode = (String) row.get("target_code");
        String toStage    = (String) row.get("to_stage");

        // null 및 타입 안전 처리
        Object raw = row.get("action_datetime");
        java.time.LocalDateTime actionAt;
        if (raw instanceof java.sql.Timestamp ts) {
            actionAt = ts.toLocalDateTime();
        } else if (raw instanceof java.time.LocalDateTime ldt) {
            actionAt = ldt;
        } else {
            actionAt = java.time.LocalDateTime.now();
        }

        String docLabel = DOC_LABEL.getOrDefault(docType, docType);
        String title    = docLabel + " - " + clientName;
        String state    = COMPLETED_STAGES.contains(toStage) ? "completed" : "pending";

        return ActivityResponse.builder()
                .date(actionAt.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .title(title)
                .detail(targetCode)
                .state(state)
                .build();
    }

    // ──────────────────────────────────────────────
    // 포맷 헬퍼
    // ──────────────────────────────────────────────

    private String formatAmount(BigDecimal value) {
        if (value == null) return "0원";
        return NumberFormat.getInstance(Locale.KOREA).format(value.longValue()) + "원";
    }

    private String formatDiff(BigDecimal diff) {
        if (diff == null || diff.compareTo(BigDecimal.ZERO) == 0) return "-";
        long v = diff.longValue();
        String prefix = v > 0 ? "+" : "";
        return prefix + NumberFormat.getInstance(Locale.KOREA).format(v) + "원";
    }

    private String formatGrowthRate(double rate) {
        if (rate == 0.0) return "-";
        String symbol = rate > 0 ? "▲" : "▼";
        return String.format("%s %.1f%%", symbol, Math.abs(rate));
    }
}